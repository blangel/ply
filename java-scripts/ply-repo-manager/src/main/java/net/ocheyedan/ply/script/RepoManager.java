package net.ocheyedan.ply.script;

import net.ocheyedan.ply.*;
import net.ocheyedan.ply.dep.Auth;
import net.ocheyedan.ply.dep.Repos;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.input.InterruptibleInputReader;
import net.ocheyedan.ply.props.*;
import net.ocheyedan.ply.script.github.Authorization;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * User: blangel
 * Date: 1/8/13
 * Time: 2:40 PM
 *
 * The default repository manager for the ply build system.
 *
 * The property file used to configure this script is {@literal repomngr.properties} and so the context is
 * {@literal repomngr}.
 * The following properties exist:
 * repositoryURI=authtype:username:encryptedpwd
 * where each {@literal repositoryURI} (there may be many entries and in fact should map to an entry within the
 * {@literal repositories.properties} file defined below) is a {@link java.net.URI} to a repository.  The
 * {@literal authtype} is (currently) only {@literal git} (but will be expanded in the future with {@literal oauth2},
 * {@literal httpbasic}, etc).  The {@literal username} is the username for authentication into the repository
 * and {@literal encryptedpwd} is the encrypted password of the user.
 *
 * Dependencies are resolved and downloaded from repositories.  The list of repositories are stored in a file
 * called {@literal repositories.properties} (so have context {@literal repositories}).  The format of each property
 * within the file is:
 * repositoryURI=type
 * where the {@literal repositoryURI} is a {@link java.net.URI} to the repository and {@literal type} is
 * the type of the repository.  Type is currently either {@literal ply} (the default so null resolves to ply) or {@literal maven}.
 * Dependencies are then resolved by appending the namespace/name/version/artifactName to the {@literal repositoryURI}.
 * For instance, if the {@literal repositoryURI} were {@literal http://repo1.maven.org/maven2} and the dependency were
 * {@literal org.apache.commons:commons-io=1.3.2:} then the dependency would be resolved by assuming the default
 * artifactName (which would be {@literal commons-io-1.3.2.jar}) and creating a link to the artifact.  If the type
 * of the repository were null or {@literal ply} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org.apache.commons/1.3.2/commons-io-1.3.2.jar}.
 * If the type were {@literal maven} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org/apache/commons/1.3.2/commons-io-1.3.2.jar}.
 * The difference between the two is that with the {@literal maven} type the dependency's {@literal namespace}'s periods
 * are resolved to forward slashes as is convention in the {@literal Maven} build system.
 *
 * The repository manager script's usage is:
 * <pre>dep add|remove|auth|auth-local</pre>
 * The {@literal add} command takes an atom and adds it as a repository for the supplied scope.
 * The {@literal remove} command takes an atom and removes it from the repositories scope, if it exists.
 * The {@literal auth} command takes an atom, an auth-type and a username (prompts for password) and stores as the authentication type for the given atom.
 * The {@literal auth-local} is the same as auth except saves the authentication information into the local {@literal repomngr} file.
 */
public final class RepoManager {

    private static final String GIT_HUB_API_NAME = "GitHub API";
    private static final String GIT_HUB_SCOPE = "repo";

    public static void main(String[] args) {
        Scope scope = Scope.named(Props.get("scope", Context.named("ply")).value());
        if ((args.length > 1) && "add".equals(args[0])) {
            addRepository(args[1], scope);
        } else if ((args.length > 1) && "remove".equals(args[0])) {
            removeRepository(args[1], scope);
        } else if ((args.length == 4) && "auth".equals(args[0])) {
            authRepository(args[1], args[2], args[3], scope, false);
        } else if ((args.length == 4) && "auth-local".equals(args[0])) {
            authRepository(args[1], args[2], args[3], scope, true);
        } else {
            usage();
            System.exit(1);
        }
    }

    private static void addRepository(String repository, Scope scope) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format [type:]repoUri.", repository);
            System.exit(1);
        }
        try {
            atom.repositoryUri.toURL();
        } catch (Exception e) {
            Output.print("^error^ Given value ^b^%s^r^ is not a valid URI and so is not a repository.",
                    atom.getPropertyName());
            System.exit(1);
        }
        PropFile repositories = load("repositories", scope, true);
        if (repositories.contains(atom.getPropertyName())) {
            Output.print("^info^ overriding repository %s; was %s now is %s.", atom.getPropertyName(),
                    repositories.get(atom.getPropertyName()).value(), atom.getPropertyValue());
            repositories.remove(atom.getPropertyName());
        }
        repositories.add(atom.getPreResolvedUri(), atom.getPropertyValue());
        store("repositories", repositories, scope, true);
        Output.print("Added repository %s%s", atom.toString(), !Scope.Default.equals(scope) ?
                String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
    }

    private static void removeRepository(String repository, Scope scope) {
        RepositoryAtom atom = null;
        try {
            atom = Repos.getExistingRepo(PlyUtil.LOCAL_CONFIG_DIR, scope, repository);
            if (atom == null) {
                return;
            }
        } catch (SystemExit se) {
            System.exit(se.exitCode);
        }

        PropFile repositories = load("repositories", scope, true);
        PropFile.Prop removed = repositories.remove(atom.getPreResolvedUri());
        store("repositories", repositories, scope, true);
        atom = RepositoryAtom.parse(String.format("%s:%s", removed.value(), removed.name)); // restore type in atom for print
        Output.print("Removed repository %s%s", atom.toString(), !Scope.Default.equals(scope) ?
                String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
    }

    private static void authRepository(String repository, String authTypeName, String username, Scope scope, boolean local) {
        Repos.AuthType authType = null;
        try {
            authType = Repos.AuthType.valueOf(authTypeName);
        } catch (Exception e) {
            Output.print("^error^ Auth-type ^b^%s^r^ not supported; only ^b^%s^r^ is supported.", authTypeName, Repos.AuthType.git.name());
            System.exit(1);
        }
        // only ensure repository entry exists if in local mode
        if (local) {
            RepositoryAtom atom;
            try {
                atom = Repos.getExistingRepo(PlyUtil.LOCAL_CONFIG_DIR, scope, repository);
                if (atom == null) {
                    addRepository(repository, scope);
                    Output.print("^warn^ Added repository ^b^%s^r^ automatically.", repository);
                    Output.print("^warn^   if this was a mistake, revert via command: ^b^ply repo remove %s^r^", repository);
                }
            } catch (SystemExit se) {
                System.exit(se.exitCode);
            }
        }

        String password = getVerifiedPassword(username);
        String encrypted = PwdUtil.encrypt(password);

        PropFile repomngr = load("repomngr", scope, local);
        File configDir = new File(getConfigDirPath(local));
        Auth auth = authType.get(username, encrypted, RepositoryAtom.parse(repository), configDir, scope);
        final Repos.AuthType type = authType;
        Output.print("Verifying authorization...");
        auth.acquireAccess(new Auth.Acquisition() {
            @Override public String getAccess(String username, String encryptedPwd) {
                switch (type) {
                    case git:
                        return getGitHubAuthToken(username, encryptedPwd);
                    default:
                        throw new AssertionError(); // should not get here, see check above
                }
            }
        });
        Repos.addAuthRepomngrProp(repomngr, repository, auth);
        store("repomngr", repomngr, scope, local);
        Output.print("Associated username/password with repository within ^b^repomngr^r^ context.");
    }

    private static String getGitHubAuthToken(String username, String encryptedPwd) {
        String existing = getExistingGitHubAuthToken(username, encryptedPwd);
        if (existing != null) {
            return existing;
        }
        String url = "https://api.github.com/authorizations";
        Authorization authorization = post(url, username, encryptedPwd, "{\"scopes\":[\"repo\"]}", Authorization.class);
        return (authorization == null ? null : authorization.getToken());
    }

    private static String getExistingGitHubAuthToken(String username, String encryptedPwd) {
        // check if the user has already authorized 'repo' for the 'GitHub API'
        String authorizationsUrl = "https://api.github.com/authorizations";
        Authorization[] authorizations = get(authorizationsUrl, username, encryptedPwd, Authorization[].class);
        if ((authorizations == null) || (authorizations.length < 1)) {
            return null;
        }
        for (Authorization authorization : authorizations) {
            if ((authorization != null) && (authorization.getApp() != null)
                    && GIT_HUB_API_NAME.equals(authorization.getApp().getName())) {
                // token must not be null
                if (authorization.getToken() == null) {
                    continue;
                }
                // must have 'repo' scope
                if ((authorization.getScopes() == null) || (authorization.getScopes().length < 1)) {
                    continue;
                }
                for (String scope : authorization.getScopes()) {
                    if (GIT_HUB_SCOPE.equals(scope)) {
                        return authorization.getToken();
                    }
                }
            }
        }
        return null;
    }

    private static <T> T post(String urlPath, String username, String encryptedPwd, String data, Class<T> clazz) {
        URL url;
        try {
            url = URI.create(urlPath).toURL();
            URLConnection urlConnection = url.openConnection();
            String pwd = PwdUtil.decrypt(encryptedPwd);
            String userpass = username + ":" + pwd;
            BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedUserpass = encoder.encode(userpass.getBytes());
            String basicAuth = "Basic " + encodedUserpass;
            urlConnection.setRequestProperty("Authorization", basicAuth);
            urlConnection.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(urlConnection.getOutputStream());
            writer.write(data);
            writer.flush();
            InputStream stream = urlConnection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
            return objectMapper.readValue(stream, clazz);
        } catch (IOException ioe) {
            Output.print(ioe);
            return null;
        }
    }

    private static <T> T get(String urlPath, String username, String encryptedPwd, Class<T> clazz) {
        URL url;
        try {
            url = URI.create(urlPath).toURL();
            URLConnection urlConnection = url.openConnection();
            String pwd = PwdUtil.decrypt(encryptedPwd);
            String userpass = username + ":" + pwd;
            BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            String encodedUserpass = encoder.encode(userpass.getBytes());
            String basicAuth = "Basic " + encodedUserpass;
            urlConnection.setRequestProperty("Authorization", basicAuth);
            InputStream stream = urlConnection.getInputStream();
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.disable(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES);
            return objectMapper.readValue(stream, clazz);
        } catch (IOException ioe) {
            return null; // don't print, allow subsequent call to post handle error printing...
        }
    }

    private static String getVerifiedPassword(String username) {
        Output.print("%s^no_line^Enter password for %s: ", PwdUtil.PWD_REQUEST_TOKEN, username);
        String pwd = getPassword();
        Output.print("%s^no_line^Reenter to verify: ", PwdUtil.PWD_REQUEST_TOKEN, username);
        String reentered = getPassword();
        if (!pwd.equals(reentered)) {
            Output.print("^error^ Passwords did not match, please retry.");
            return getVerifiedPassword(username);
        } else {
            return pwd;
        }
    }

    private static String getPassword() {
        String answer;
        InterruptibleInputReader reader = new InterruptibleInputReader(System.in);
        try {
            answer = reader.readLine();
        } catch (IOException ioe) {
            Output.print("%s^no_line^^error^ When entering password, please retry: ", PwdUtil.PWD_REQUEST_TOKEN);
            return getPassword();
        }
        if ((answer == null) || answer.isEmpty()) {
            Output.print("%s^no_line^^error^ Password may not be blank, please retry: ", PwdUtil.PWD_REQUEST_TOKEN);
            return getPassword();
        }
        return answer;
    }

    private static PropFile load(String name, Scope scope, boolean local) {
        String path = getPath(name, scope, local);
        return PropFiles.load(path, true, false);
    }

    private static void store(String name, PropFile props, Scope scope, boolean local) {
        String path = getPath(name, scope, local);
        if (!PropFiles.store(props, path, true)) {
            System.exit(1);
        }
    }

    private static String getConfigDirPath(boolean local) {
        if (local) {
            String localDir = Props.get("project.dir", Context.named("ply")).value();
            return FileUtil.pathFromParts(localDir, "config");
        } else {
            String systemDir = System.getProperty("ply.home");
            return FileUtil.pathFromParts(systemDir, "config");
        }
    }

    private static String getPath(String name, Scope scope, boolean local) {
        String configDirPath = getConfigDirPath(local);
        return FileUtil.pathFromParts(configDirPath, name + scope.getFileSuffix() + ".properties");
    }

    private static void usage() {
        Output.print("repo <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^add <repo-atom>^r^ : adds repo-atom to the list of repositories.");
        Output.print("    ^b^remove <repo-atom>^r^ : removes repo-atom from the list of repositories.");
        Output.print("    ^b^auth <repo-atom> <auth-type> <username>^r^ : adds authentication information for repo-atom (prompting for password).");
        Output.print("    ^b^auth-local <repo-atom> <auth-type> <username>^r^ : same as auth but stores the information within the project configuration.");
        Output.print("  ^b^repo-atom^r^ is [type:]repoURI (type is optional and defaults to ply, must be either ply or maven).");
        Output.print("  ^b^auth-type^r^ must be 'git' at this time (in future, 'oauth2', 'httpbasic' etc may be added).");
        Output.print("  Repositories can be grouped by ^b^scope^r^ (i.e. test).  The default scope is null.");
    }

}
