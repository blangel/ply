package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.props.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * User: blangel
 * Date: 1/4/13
 * Time: 11:31 AM
 *
 * Utility class to interact with {@literal repository} urls.
 */
public final class Repos {

    /**
     * The repository authorization types supported by ply.
     */
    public static enum AuthType {

        /* uses a git repo as a ply repository */
        git,
        /* uses an http/https basic-auth repository */
        basic;

        public Auth get(String username, String encryptedPwd, RepositoryAtom atom, File configDir, Scope scope) {
            switch (this) {
                case git:
                    return new GitHubAuth(username, encryptedPwd, atom, configDir, scope);
                case basic:
                    return new BasicAuth(username, encryptedPwd, atom);
                default:
                    throw new AssertionError("Unknown repo auth-type");
            }
        }
    }

    /**
     * Resolves {@code repositoryAtom} to a canonical directory path and returns that value.
     * @param repositoryAtom assumed to be a {@link RepositoryAtom} to a local directory
     * @return the canonical directory path of {@code repositoryAtom}
     */
    public static String getDirectoryPathForRepo(RepositoryAtom repositoryAtom) {
        try {
            String repoPath = repositoryAtom.getPropertyName();
            repoPath = FileUtil.stripFileUriPrefix(repoPath);
            if (repoPath.contains(":")) { // non-file uri; keep as is
                return repoPath;
            }
            return FileUtil.getCanonicalPath(new File(repoPath));
        } catch (RuntimeException re) {
            // the path is likely invalid, attempt resolution anyway and let the subsequent code determine the
            // actual reason the path is invalid.
        }
        return repositoryAtom.getPropertyName();
    }

    /**
     * Copies the project artifact ({@literal project.artifact.name} from {@literal project.build.dir}) into
     * {@code localRepo}
     * @param scope for which to pull the necessary properties
     * @param localRepo into which to install the artifact
     * @return true on success, false if the artifact does not exist or there was an error saving
     */
    public static boolean install(Scope scope, RepositoryAtom localRepo) {
        String artifactName = Props.get("artifact.name", Context.named("project"), scope).value();
        DependencyAtom dependencyAtom = Deps.getProjectDep();
        return install(scope, localRepo, artifactName, dependencyAtom);
    }

    /**
     * Copies the {@code artifactName} from {@literal project.build.dir} into {@code localRepo}
     * @param scope for which to pull the necessary properties
     * @param localRepo into which to install the artifact
     * @param artifactName of the file to copy
     * @return true on success, false if the artifact does not exist or there was an error saving
     */
    public static boolean install(Scope scope, RepositoryAtom localRepo, String artifactName, DependencyAtom dependencyAtom) {
        String buildDirPath = Props.get("build.dir", Context.named("project"), scope).value();
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return false;
        }

        String plyProjectDirPath = Props.get("project.dir", Context.named("ply"), scope).value();
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", String.format("dependencies%s.properties", scope.getFileSuffix()));

        return installArtifact(scope, artifact, dependenciesFile, dependencyAtom, localRepo);
    }

    /**
     * Copies the project artifact ({@literal project.artifact.name} from {@literal project.build.dir}) into
     * {@code localRepo}
     * @param scope for which to pull the necessary properties
     * @param artifact from which to copy into {@code localRepo}
     * @param dependenciesFile the dependencies file (or empty) associated with {@code artifact}
     * @param dependencyAtom representing meta-information about {@code artifact}
     * @param localRepo into which to install the {@code artifact}
     * @return true on success, false if the artifact does not exist or there was an error saving
     */
    public static boolean installArtifact(Scope scope, File artifact, File dependenciesFile, DependencyAtom dependencyAtom,
                                          RepositoryAtom localRepo) {
        String localRepoDirPath = Deps.getDependencyDirectoryPathForRepo(dependencyAtom, localRepo);
        String localRepoArtifactPath = Deps.getDependencyArtifactPathForRepo(dependencyAtom, localRepo);
        File localRepoArtifact = new File(localRepoArtifactPath);
        if (!FileUtil.copy(artifact, localRepoArtifact)) {
            return false;
        }
        String artifactsScopeName = Props.get("artifacts.label", Context.named("project"), scope).value();
        Scope artifactsScope = Scope.named(artifactsScopeName);
        if (!Scope.Default.equals(artifactsScope)) {
            String artifactsScopeFile = String.format("%s.artifacts.label", dependencyAtom.getArtifactName());
            File localRepoArtifactsScopePath = FileUtil.fromParts(localRepoDirPath, artifactsScopeFile);
            PropFile artifactsScopePropFile = new PropFile(Context.named(artifactsScopeFile), scope, PropFile.Loc.Local);
            artifactsScopePropFile.add("artifacts.label", artifactsScopeName);
            if (!PropFiles.store(artifactsScopePropFile, localRepoArtifactsScopePath.getPath(), true)) {
                return false;
            }
        }

        PropFile checksum = new PropFile(Context.named("checksum"), artifactsScope, PropFile.Loc.Local);
        File localRepoChecksumFile = FileUtil.fromParts(localRepoDirPath, String.format("checksum%s.properties", artifactsScope.getFileSuffix()));
        String artifactChecksum = FileUtil.getSha1Hash(localRepoArtifact);
        checksum.add("artifact", artifactChecksum);
        if (!addDependenciesChecksumValues(scope, checksum, dependenciesFile)) {
            return false;
        }
        checksum.add("timestamp", String.format("%d", System.currentTimeMillis()));
        if (!PropFiles.store(checksum, localRepoChecksumFile.getPath(), true)) {
            return false;
        }

        File localRepoDependenciesFile = FileUtil.fromParts(localRepoDirPath, String.format("dependencies%s.properties", artifactsScope.getFileSuffix()));
        if ((dependenciesFile != null) && dependenciesFile.exists()) {
            return FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        } else {
            // need to override (perhaps there were dependencies but now none.
            PropFile dependencies = new PropFile(Context.named("dependencies"), artifactsScope, PropFile.Loc.Local);
            return PropFiles.store(dependencies, localRepoDependenciesFile.getPath(), true);
        }
    }

    private static boolean addDependenciesChecksumValues(Scope scope, PropFile checksum, File dependenciesFile) {
        PropFile resolvedDependencies = Deps.getResolvedProperties(true);
        if (resolvedDependencies == null) {
            if ((dependenciesFile != null) && dependenciesFile.exists()) {
                Output.print("^error^ Could not find the resolved-deps.properties file; dependencies must be resolved to compute checksum.");
                return false;
            } else {
                return true; // project has no dependencies
            }
        }
        for (PropFile.Prop prop : resolvedDependencies.props()) {
            String sha1;
            PropFile checksumPropFile = new PropFile(Context.named("checksum"), PropFile.Loc.Local);
            String checksumFilePath = createChecksumFile(scope, prop.value());
            if (checksumFilePath == null) {
                return false;
            }
            PropFiles.load(checksumFilePath, checksumPropFile, true);
            if (checksumPropFile.contains("artifact")) {
                sha1 = checksumPropFile.get("artifact").value();
            } else {
                sha1 = FileUtil.getSha1Hash(new File(prop.value()));
                checksumPropFile.add("artifact", sha1);
                PropFiles.store(checksumPropFile, checksumFilePath, true);
            }
            checksum.add(prop.name, sha1);
        }
        return true;
    }

    private static String createChecksumFile(Scope scope, String path) {
        if (path == null) {
            return null;
        }
        int index = path.lastIndexOf(File.separatorChar);
        if (index == -1) {
            return null;
        }
        String artifactsLabelName = Props.get("artifacts.label", Context.named("project"), scope).value();
        Scope artifactsLabel = Scope.named(artifactsLabelName);
        return FileUtil.pathFromParts(path.substring(0, index), String.format("checksum%s.properties", artifactsLabel.getFileSuffix()));
    }

    /**
     * Constructs a {@link RepositoryRegistry} for all repositories defined within the {@code configDirectory} and
     * {@code scope}.  If {@code syntheticDependencyKey} is not null maps it to {@code syntheticDependencies} as a
     * synthetic repository within the returned {@link RepositoryRegistry}.
     * @param configDirectory for which to pull the repositories properties
     * @param scope for which to pull the repositories properties
     * @param syntheticDependencyKey if not null the dependency key into a synthetic repository containing
     *                               {@code syntheticDependencies}
     * @param syntheticDependencies the dependencies to be used if {@code syntheticDependencyKey} is not null
     * @return a {@link RepositoryRegistry} containing all the {@link RepositoryAtom} values
     *         defined by the {@literal repositories.properties} for this invocation.
     * @throws SystemExit if the local repository cannot be found.
     */
    public static RepositoryRegistry createRepositoryRegistry(File configDirectory, Scope scope,
                                                              DependencyAtom syntheticDependencyKey,
                                                              List<DependencyAtom> syntheticDependencies)
            throws SystemExit {
        PropFile.Prop localRepoProp = Props.get("localRepo", Context.named("depmngr"), scope, configDirectory);
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp.value());
        if (localRepo == null) {
            if (PropFile.Prop.Empty.equals(localRepoProp)) {
                Output.print("^error^ No ^b^localRepo^r^ property defined (^b^ply set localRepo=xxxx in depmngr^r^).");
            } else {
                Output.print("^error^ Could not resolve directory for ^b^localRepo^r^ property [ is ^b^%s^r^ ].", localRepoProp.value());
            }
            throw new SystemExit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        PropFileChain repositories = Props.get(Context.named("repositories"), scope, configDirectory);
        if (repositories != null) {
            for (PropFile.Prop repoProp : repositories.props()) {
                String repoUri = repoProp.name;
                if (localRepo.getPropertyName().equals(repoUri)) {
                    continue;
                }
                String repoType = repoProp.value();
                String repoAtom = repoType + ":" + repoUri;
                RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
                if (repo == null) {
                    Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
                } else {
                    Auth auth = getAuth(configDirectory, scope, repo);
                    repo.setAuth(auth);
                    repositoryAtoms.add(repo);
                }
            }
        }
        Collections.sort(repositoryAtoms, RepositoryAtom.LOCAL_COMPARATOR);
        Map<DependencyAtom, List<DependencyAtom>> synthetic = null;
        if (syntheticDependencyKey != null) {
            synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
            synthetic.put(syntheticDependencyKey, syntheticDependencies);
        }
        return new RepositoryRegistry(localRepo, repositoryAtoms, synthetic);
    }

    /**
     * Resolves {@code repository} to a {@link RepositoryAtom}
     * @param configDir for property resolution
     * @param scope for property resolution
     * @param repository to resolve
     * @return null if {@code repository} is parsable as a {@link RepositoryAtom} but is not an existing
     *         repository entry
     * @throws SystemExit on parsing of {@code repository} failure
     */
    public static RepositoryAtom getExistingRepo(File configDir, Scope scope, String repository) throws SystemExit {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format [type:]repoUri.", repository);
            throw new SystemExit(1);
        }

        PropFile.Prop found = Props.get(atom.getPreResolvedUri(), Context.named("repositories"), scope, configDir);
        if (PropFile.Prop.Empty.equals(found)) {
            // before failing, check that this isn't a resolved unix-tilde path
            String toTry = FileUtil.reverseUnixTilde(atom.getPreResolvedUri());
            RepositoryAtom toTryAtom = RepositoryAtom.parse(toTry);
            found = (toTryAtom == null ? PropFile.Prop.Empty :
                    Props.get(toTryAtom.getPreResolvedUri(), Context.named("repositories"), scope, configDir));
            if (PropFile.Prop.Empty.equals(found)) {
                Output.print("^warn^ Repository not found; given %s", repository);
                return null;
            } else {
                atom = toTryAtom;
            }
        }
        return atom;
    }

    /**
     * @param configDir from which to resolve properties
     * @param scope from which to resolve properties
     * @param repositoryAtom to which to match the associated entry within {@link Context} {@literal repomngr}
     * @return the {@link PropFile.Prop} within {@link Context} {@literal repomngr} associated with {@code repositoryAtom}
     */
    public static PropFile.Prop getAuthPropFromRepo(File configDir, Scope scope, RepositoryAtom repositoryAtom) {
        String repository = repositoryAtom.getPreResolvedUri();
        PropFile.Prop prop = Props.get(repository, Context.named("repomngr"), scope, configDir);
        if (PropFile.Prop.Empty.equals(prop)) {
            String toTry;
            if (repository.startsWith("~")) {
                toTry = FileUtil.resolveUnixTilde(repository);
            } else if (repository.startsWith("/")) {
                toTry = FileUtil.reverseUnixTilde(repository);
            } else {
                return prop;
            }
            return Props.get(toTry, Context.named("repomngr"), scope, configDir);
        }
        return prop;
    }

    /**
     * Constructs an authorization property for the {@code repomngr} where the {@code repository} is the property
     * key and the property value is the concatenation (separated with colon characters) of {@code auth.authType},
     * {@code auth.username} and {@code auth.encryptedPwd}.
     * @param repomngr in which to add the property entry
     * @param repository property key
     * @param auth authorization information
     * @return the created {@link PropFile.Prop} value
     */
    public static PropFile.Prop addAuthRepomngrProp(PropFile repomngr, String repository, Auth auth) {
        String propertyValue = auth.getPropertyValue();
        if (repomngr.contains(repository)) {
            repomngr.remove(repository);
        }
        return repomngr.add(repository, propertyValue);
    }

    /**
     * @param configDir for which to load properties
     * @param scope for which to load properties
     * @param repositoryAtom the repository for which to look for authentication information
     * @return the {@link Auth} associated with {@code repositoryAtom} or null if none exists or there were issues parsing
     *         the stored value
     */
    public static Auth getAuth(File configDir, Scope scope, RepositoryAtom repositoryAtom) {
        String repository = repositoryAtom.getPreResolvedUri();
        PropFile.Prop prop = getAuthPropFromRepo(configDir, scope, repositoryAtom);
        if (PropFile.Prop.Empty.equals(prop)) {
            return null;
        } else {
            String value = prop.value();
            int index = value.indexOf(':');
            if (index == -1) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String type = value.substring(0, index);
            AuthType authType;
            try {
                authType = AuthType.valueOf(type);
            } catch (Exception e) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but invalid auth-type [ %s ], ignoring", repository, value);
                return null;
            }
            int usernameIndex = value.indexOf(':', index + 1);
            if (usernameIndex == -1) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String username = value.substring(index + 1, usernameIndex);
            if (usernameIndex >= (value.length() - 1)) {
                Output.print("^warn^ Found auth setting for repo [ %s ] but could not parse it [ %s ], ignoring", repository, value);
                return null;
            }
            String encryptedPwd = value.substring(usernameIndex + 1);
            return authType.get(username, encryptedPwd, repositoryAtom, configDir, scope);
        }
    }

    private Repos() { }

}
