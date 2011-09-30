package org.moxie.ply.script;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/29/11
 * Time: 9:41 AM
 *
 * The default dependency manager for the ply build system.
 * The property file used to configure this script is {@literal depmngr.properties} and so the context is {@literal depmngr}.
 * The following properties exist:
 * localRepo=string [[default=${PLY_HOME}/repo]] (this is the local repository where remote dependencies will
 *           be stored.  Note, multiple local-filesystem repositories may exist within {@literal repositories.properties}
 *           but only this, the {@literal localRepo}, will be used to store remote repositories' downloads.  The format
 *           is repoUri[::type], see below for description of this format).
 *
 * Dependency information is stored in a file called {@literal dependencies.properties} and so the context is
 * {@literal dependencies}.  The format of each property within the file is:
 * namespace::name=version::artifactName
 * where namespace provides a unique context for name.  It is analogous to {@literal groupId} in {@literal Maven} or
 * the {@literal category} portion of a base atom in {@literal portage}.
 * The name is the name of the project.  It is analogous to {@literal artifactId} in {@literal Maven} or the
 * {@literal packagename} portion of a base atom in {@literal portage}.
 * The version is the version of the project.  It is analogous to {@literal version} in {@literal Maven} or the
 * atom version in {@literal portage}.
 * TODO (perhaps not) - The version portion may be null. Leaving/setting the version to null for a dependency is
 * TODO - interpreted as meaning find and use the latest version of the dependency.  The latest version is that saved
 * TODO = into the repository last.
 * The artifactName is the file name of the dependency.  It may be null and if so is assumed to be {@literal name-version.jar}
 * where {@literal name} and {@literal version} correspond to the definition of the dependency.
 *
 * Dependencies are resolved and downloaded from repositories.  The list of repositories are stored in a file
 * called {@literal repositories.properties} (so have context {@literal repositories}).  The format of each property
 * within the file is:
 * repositoryURI=type
 * where the {@literal repositoryURI} is a {@link java.net.URI} to the repository and {@literal type} is
 * the type of the repository.  Type is currently either {@literal ply} (the default so null resolves to ply) or {@literal maven}.
 * Dependencies are then resolved by appending the namespace/name/version/artifactName to the {@literal repositoryURI}.
 * For instance, if the {@literal repositoryURI} were {@literal http://repo1.maven.org/maven2} and the dependency were
 * {@literal org.apache.commons::commons-io=1.3.2::} then the dependency would be resolved by assuming the default
 * artifactName (which would be {@literal commons-io-1.3.2.jar}) and creating a link to the artifact.  If the type
 * of the repository were null or {@literal ply} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org.apache.commons/1.3.2/commons-io-1.3.2.jar}.
 * If the type were {@literal maven} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org/apache/commons/1.3.2/commons-io-1.3.2.jar}.
 * The difference between the two is that with the {@literal maven} type the dependency's {@literal namespace}'s periods
 * are resolved to forward slashes as is convention in the {@literal Maven} build system.
 *
 * The dependency script's usage is:
 * <pre>dep [--usage] [add|remove|add-repo|remove-repo]</pre>
 * where {@literal --usage} prints the usage information.
 * The {@literal add} command takes an atom and adds it as a dependency, resolving it eagerly from the known repos and
 * failing if it cannot be resolved.
 * The {@literal remove} command takes an atom and removes it from the dependencies, if it exists.
 * The {@literal add-repo} command takes a repository and adds it to the repositories.
 * The {@literal remove-repo} command removes the repository.
 * If nothing is passed to the script then dependency resolution is done for all dependencies against the known
 * repositories.
 */
public class DependencyManager {

    /**
     * Represents a dependency atom made up of namespace::name::version[::artifactName]
     * If artifactName is null then (name-version.jar) will be used when necessary.
     */
    public static final class DependencyAtom {
        public final String namespace;
        public final String name;
        public final String version;
        public final String artifactName;

        public DependencyAtom(String namespace, String name, String version, String artifactName) {
            this.namespace = namespace;
            this.name = name;
            this.version = version;
            this.artifactName = artifactName;
        }

        public DependencyAtom(String namespace, String name, String version) {
            this(namespace, name, version, null);
        }

        public String getPropertyName() {
            return namespace + "::" + name;
        }

        public String getPropertyValue() {
            return (version == null ? "" : version) + (artifactName != null ? "::" + artifactName : "");
        }

        public String getResolvedPropertyValue() {
            return version + "::" + getArtifactName();
        }

        public String getArtifactName() {
            return (artifactName == null ? name + "-" + version + ".jar" : artifactName);
        }

        @Override public String toString() {
            return getPropertyName() + "::" + getResolvedPropertyValue();
        }

        public static DependencyAtom parse(String atom, AtomicReference<String> error) {
            String[] parsed = atom.split("::");
            if ((parsed.length < 3) || (parsed.length > 4)) {
                if (error != null) {
                    switch (parsed.length) {
                        case 0: error.set("namespace, name and version"); break;
                        case 1: error.set("name and version"); break;
                        default: error.set("version");
                    }
                }
                return null;
            }
            return (parsed.length == 3 ? new DependencyAtom(parsed[0], parsed[1], parsed[2]) :
                    new DependencyAtom(parsed[0], parsed[1], parsed[2], parsed[3]));
        }
    }

    /**
     * Represents a repository atom made up of repositoryURI[::type].
     * If type is null then ply will be used when necessary.
     */
    public static final class RepositoryAtom {
        public static enum Type {
            ply, maven
        }
        public final URI repositoryUri;
        public final Type type;

        public RepositoryAtom(URI repositoryUri, Type type) {
            this.repositoryUri = repositoryUri;
            this.type = type;
        }
        public RepositoryAtom(URI repositoryUri) {
            this(repositoryUri, null);
        }
        public String getPropertyName() {
            return repositoryUri.toString();
        }
        public String getPropertyValue() {
            return (type == null ? "" : type.name());
        }
        public Type getResolvedType() {
            return (type == null ? Type.ply : type);
        }
        public String getResolvedPropertyValue() {
            return getResolvedType().name();
        }
        @Override public String toString() {
            return getPropertyName() + "::" + getResolvedPropertyValue();
        }
        public static RepositoryAtom parse(String atom) {
            if (atom == null) {
                return null;
            }
            String[] resolved = atom.split("::");
            if ((resolved.length < 1) && (resolved.length > 2)) {
                return null;
            }
            URI repositoryUri = URI.create(resolved[0]);
            Type type = null;
            if (resolved.length == 2) {
                if ("ply".equals(resolved[1])) {
                    type = Type.ply;
                } else if ("maven".equals(resolved[1])) {
                    type = Type.maven;
                } else {
                    System.out.printf("^warn^ unsupported type %s, must be either null, ply or maven.\n", resolved[1]);
                }
            }
            return new RepositoryAtom(repositoryUri, type);
        }
    }

    public static void main(String[] args) {
        if ((args == null) || (args.length > 0 && "--usage".equals(args[0]))) {
            usage();
            return;
        }
        if ((args.length > 1) && "add".equals(args[0])) {
            addDependency(args[1]);
        } else if ((args.length > 1) && "remove".equals(args[0])) {
            removeDependency(args[1]);
        } else if ((args.length > 1) && "add-repo".equals(args[0])) {
            addRepository(args[1]);
        } else if ((args.length > 1) && "remove-repo".equals(args[0])) {
            removeRepository(args[1]);
        } else if (args.length == 0) {
            System.out.println("Resolving dependencies...");
            resolveDependencies();
            System.out.println("Resolved dependencies.");
        } else {
            usage();
        }
    }

    private static void addDependency(String dependency) {
        AtomicReference<String> error = new AtomicReference<String>(null);
        DependencyAtom atom = DependencyAtom.parse(dependency, error);
        if (atom == null) {
            System.out.printf("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace::name::version[::artifactName]).\n",
                    dependency, error.get());
            System.exit(1);
        }
        Properties dependencies = loadDependenciesFile();
        if (dependencies.contains(atom.getPropertyName())) {
            System.out.printf("^info^ overriding dependency %s; was %s now is %s.\n", atom.getPropertyName(),
                    dependencies.getProperty(atom.getPropertyName()), atom.getPropertyValue());
        }
        dependencies.put(atom.getPropertyName(), atom.getPropertyValue());
        if (resolveDependency(atom)) {
            storeDependenciesFile(dependencies);
        } else {
            System.out.printf("^error^ unable to resolve dependency ^b^%s^r^. Ensure you are able to connect to the remote repositories.\n", atom.toString());
            System.exit(1);
        }
    }

    private static void removeDependency(String dependency) {
        DependencyAtom atom = DependencyAtom.parse(dependency, null);
        if (atom == null) {
            // allow non-version specification.
            String[] split = dependency.split("::");
            if (split.length < 2) {
                System.out.printf("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace::name[::version::artifactName]).\n",
                        dependency, (split.length == 1 ? "name" : "namespace and name"));
                System.exit(1);
            }
            atom = new DependencyAtom(split[0], split[1], null);
        }
        if (System.getenv("dependencies." + atom.getPropertyName()) == null) {
            System.out.printf("^warn^ Dependency not found; given %s::%s\n", atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            Properties dependencies = loadDependenciesFile();
            dependencies.remove(atom.getPropertyName());
            storeDependenciesFile(dependencies);
        }
    }

    private static void addRepository(String repository) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            System.out.printf("^error^ Repository %s not of format repoUri[::type].\n", repository);
            System.exit(1);
        }
        try {
            atom.repositoryUri.toURL();
        } catch (Exception e) {
            System.out.printf("^error^ Given value ^b^%s^r^ is not a valid URL and so is not a repository.\n",
                    atom.getPropertyName());
            System.exit(1);
        }
        Properties repositories = loadRepositoriesFile();
        if (repositories.contains(atom.getPropertyName())) {
            System.out.printf("^info^ overriding repository %s; was %s now is %s.\n", atom.getPropertyName(),
                    repositories.getProperty(atom.getPropertyName()), atom.getPropertyValue());
        }
        repositories.put(atom.getPropertyName(), atom.getPropertyValue());
        storeRepositoriesFile(repositories);
    }

    private static void removeRepository(String repository) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            System.out.printf("^error^ Repository %s not of format repoUri[::type].\n", repository);
            System.exit(1);
        }

        if (System.getenv("repositories." + atom.getPropertyName()) == null) {
            System.out.printf("^warn^ Repository not found; given %s::%s\n", atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            Properties repositories = loadRepositoriesFile();
            repositories.remove(atom.getPropertyName());
            storeRepositoriesFile(repositories);
        }
    }

    private static void resolveDependencies() {
        Properties dependencies = loadDependenciesFile();
        AtomicReference<String> error = new AtomicReference<String>();
        List<RepositoryAtom> repositoryAtoms = createRepositoryList();
        for (String dependencyKey : dependencies.stringPropertyNames()) {
            error.set(null);
            String dependencyValue = dependencies.getProperty(dependencyKey);
            DependencyAtom dependencyAtom = DependencyAtom.parse(dependencyKey + "::" + dependencyValue, error);
            if (dependencyAtom == null) {
                System.out.printf("^warn^ Invalid dependency %s::%s; missing %s\n", dependencyKey, dependencyValue,
                        error.get());
                continue;
            }
            resolveDependency(dependencyAtom, repositoryAtoms);
        }
    }

    private static boolean resolveDependency(DependencyAtom dependencyAtom) {
        return resolveDependency(dependencyAtom, createRepositoryList());
    }

    private static boolean resolveDependency(DependencyAtom dependencyAtom, List<RepositoryAtom> repositories) {
        if (repositories.size() < 1) {
            throw new IllegalArgumentException("Need at least one repository!");
        }
        RepositoryAtom localRepo = repositories.get(0);
        List<RepositoryAtom> nonLocalRepos = repositories.subList(1, repositories.size());

        // build the url to the dependency for the local repo.
        String localPath = getDependencyPathForRepo(dependencyAtom, localRepo);
        URL localUrl = getUrl(localPath);
        if (localUrl == null) {
            return false;
        }
        String localDirPath = getDependencyDirectoryPathForRepo(dependencyAtom, localRepo);
        File localDepDirFile = new File(localDirPath.substring(7));
        File localDepFile = new File(localUrl.getFile());
        if (localDepFile.exists()) {
            // TODO - transitive, as it could exist but they've been deleted from the repo
            return true;
        }

        // now check each repo.
        for (RepositoryAtom remoteRepo : nonLocalRepos) {
            String remotePath = getDependencyPathForRepo(dependencyAtom, remoteRepo);
            URL remoteUrl = getUrl(remotePath);
            if (remoteUrl == null) {
                continue;
            }
            if (copy(remoteUrl, localDepFile, localDepDirFile)) {
                // TODO - transitive
                return true;
            }
        }
        System.out.printf("^warn^ Dependency ^b^%s^r^ not found in any repository.\n", dependencyAtom.toString());
        return false;
    }

    private static String getDependencyPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String dependencyDirectoryPath = getDependencyDirectoryPathForRepo(dependencyAtom, repositoryAtom);
        return dependencyDirectoryPath + File.separator + dependencyAtom.getArtifactName();
    }

    private static String getDependencyDirectoryPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String startPath = repositoryAtom.getPropertyName();
        if (!startPath.contains(":")) {
            // a file path without prefix, make absolute
            startPath = "file://" + startPath;
        }
        // hygiene the end separator
        if (!startPath.endsWith("/") && !startPath.endsWith("\\")) {
            startPath = startPath + File.separator;
        }
        RepositoryAtom.Type type = repositoryAtom.getResolvedType();
        String endPath = (type == RepositoryAtom.Type.ply ? dependencyAtom.namespace :
                dependencyAtom.namespace.replaceAll("\\.", File.separator))
                + File.separator + dependencyAtom.name + File.separator +
                dependencyAtom.version;
        // hygiene the start separator
        if (endPath.startsWith("/") || endPath.startsWith("\\")) {
            endPath = endPath.substring(1, endPath.length());
        }
        return startPath + endPath;
    }

    private static URL getUrl(String path) {
        try {
            System.out.printf("^info^ resolving %s\n", path);
            return new URI(path).toURL();
        } catch (URISyntaxException urise) {
            System.out.printf("^error^ %s\n", urise.getMessage());
        } catch (MalformedURLException murle) {
            System.out.printf("^error^ %s\n", murle.getMessage());
        }
        return null;
    }

    private static boolean copy(URL from, File to, File toDir) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            // ensure destination exists.
            toDir.mkdirs();
            to.createNewFile();

            inputStream = new BufferedInputStream(from.openStream());
            outputStream = new BufferedOutputStream(new FileOutputStream(to));

            byte[] buffer = new byte[2048];
            int read = 0;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            return true;
        } catch (IOException ioe) {
            System.out.printf("^error^ %s\n", ioe.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
        return false;
    }

    private static List<RepositoryAtom> createRepositoryList() {
        RepositoryAtom localRepo = RepositoryAtom.parse(System.getenv("depmngr.localRepo"));
        if (localRepo == null) {
            System.out.println("^error^ Local repository not defined.  Set 'localRepo' property in context 'depmngr'");
            System.exit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        repositoryAtoms.add(localRepo);
        for (String environmentKey : System.getenv().keySet()) {
            if (environmentKey.startsWith("repositories.")) {
                String repoUri = environmentKey.replace("repositories.", "");
                if (localRepo.getPropertyName().equals(repoUri)) {
                    continue;
                }
                String repoType = System.getenv(environmentKey);
                String repoAtom = repoUri + "::" + repoType;
                RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
                if (repo == null) {
                    System.out.printf("^warn^ Invalid repository declared %s, ignoring.\n", repoAtom);
                } else {    
                    repositoryAtoms.add(repo);
                }
            }
        }
        return repositoryAtoms;
    }

    private static Properties loadDependenciesFile() {
        String localDir = System.getenv("ply.project.dir");
        return loadFile(localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies.properties");
    }

    private static Properties loadRepositoriesFile() {
        String localDir = System.getenv("ply.project.dir");
        return loadFile(localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties");
    }

    private static Properties loadFile(String path) {
        Properties properties = new Properties();
        File file = new File(path);
        FileInputStream fileInputStream = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fileInputStream = new FileInputStream(file);
            properties.load(fileInputStream);
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.exit(1);
        } finally {
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
        return properties;
    }

    private static void storeDependenciesFile(Properties dependencies) {
        String localDir = System.getenv("ply.project.dir");
        storeFile(dependencies, localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies.properties");
    }

    private static void storeRepositoriesFile(Properties repositories) {
        String localDir = System.getenv("ply.project.dir");
        storeFile(repositories, localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties");
    }

    private static void storeFile(Properties properties, String path) {
        File file = new File(path);
        FileOutputStream fileOutputStream = null;
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            fileOutputStream = new FileOutputStream(file);
            properties.store(fileOutputStream, null);
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());
            System.exit(1);
        } catch (IOException ioe) {
            System.out.println(ioe.getMessage());
            System.exit(1);
        } finally {
            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
    }

    private static void usage() {
        System.out.println("dep [--usage] <^b^command^r^>");
        System.out.println("  where ^b^command^r^ is either:");
        System.out.println("    ^b^add <dep-atom>^r^\t: adds dep-atom to the list of dependencies (or replacing the version if it already exists).");
        System.out.println("    ^b^remove <dep-atom>^r^\t: removes dep-atom from the list of dependencies.");
        System.out.println("    ^b^add-repo <rep-atom>^r^\t: adds rep-atom to the list of repositories.");
        System.out.println("    ^b^remove-repo <rep-atom>^r^: removes rep-atom from the list of repositories.");
        System.out.println("  ^b^dep-atom^r^ is namespace::name::version[::artifactName] (artifactName is optional and defaults to name-version.jar).");
        System.out.println("  ^b^rep-atom^r^ is repoURI[::type] (type is optional and defaults to ply, must be either ply or maven).");
        System.out.println("  if no command is passed then dependency resolution is done for all dependencies against the known repositories.");
    }

}
