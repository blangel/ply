package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PwdUtil;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.props.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 1/9/13
 * Time: 7:30 PM
 */
final class GitHubAuth implements Auth {

    private final String username;

    private final String encryptedPwd;

    private final RepositoryAtom repositoryAtom;

    private final Map<String, String> headers;

    private final File configDir;

    private final Scope scope;

    GitHubAuth(String username, String encryptedPwd, RepositoryAtom repositoryAtom, File configDir, Scope scope) {
        this.username = username;
        this.encryptedPwd = encryptedPwd;
        this.repositoryAtom = repositoryAtom;
        this.headers = new HashMap<String, String>(2, 1.0f);
        this.configDir = configDir;
        this.scope = scope;
    }

    @Override public String getUsername() {
        return username;
    }

    @Override public String getEncryptedPwd() {
        return encryptedPwd;
    }

    @Override public RepositoryAtom getRepositoryAtom() {
        return repositoryAtom;
    }

    @Override public String getPropertyValue() {
        return String.format("git:%s:%s", username, encryptedPwd);
    }

    @Override public String getArtifactPath(String remotePathDir, DependencyAtom dependencyAtom) {
        return FileUtil.pathFromParts(remotePathDir, dependencyAtom.getArtifactName());
    }

    @Override public String getDependenciesPath(String remotePathDir, String name) {
        return FileUtil.pathFromParts(remotePathDir, name);
    }

    @Override public Map<String, String> getHeaders() {
        if (headers.isEmpty()) {
            String authToken = getAuthToken(configDir, scope);
            if (authToken == null) {
                Output.print("^error^ Could not get git-hub auth token for ^b^%s^r^ in repo ^b^%s^r^", username, repositoryAtom.getPreResolvedUri());
                Output.print("^error^ Fix by running command: ^b^ply repo auth %s git %s^r^", repositoryAtom.getPreResolvedUri(), username);
                System.exit(1);
            }
            headers.put("Authorization", String.format("token %s", authToken));
            headers.put("Accept", "application/vnd.github.v3.raw");
        }
        return headers;
    }

    @Override public void acquireAccess(Auth.Acquisition acquisition) {
        if (!createAuthToken(acquisition)) {
            Output.print("^error^ Could not create git-hub auth token for ^b^%s^r^ in repo ^b^%s^r^", username, repositoryAtom.getPreResolvedUri());
            Output.print("^error^ Perhaps you entered the wrong password?");
            System.exit(1);
        }
    }

    private String getAuthToken(File configDir, Scope scope) {
        PropFile.Prop authToken = Props.get(username, Context.named("repogithub"), scope, configDir);
        if (PropFile.Prop.Empty.equals(authToken)) {
            return null;
        } else {
            return PwdUtil.decrypt(authToken.value());
        }
    }

    private boolean createAuthToken(Auth.Acquisition acquisition) {
        String authToken = acquisition.getAccess(username, encryptedPwd);
        if (authToken == null) {
            return false;
        }
        String encrypted = PwdUtil.encrypt(authToken);
        String propsFilePath = FileUtil.pathFromParts(configDir.getPath(), "repogithub" + scope.getFileSuffix() + ".properties");
        PropFile repogithub = PropFiles.load(propsFilePath, true, false);
        if (repogithub.contains(username)) {
            repogithub.remove(username);
        }
        repogithub.add(username, encrypted);
        return PropFiles.store(repogithub, propsFilePath, true);
    }
}
