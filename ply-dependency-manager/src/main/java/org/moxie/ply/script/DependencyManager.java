package org.moxie.ply.script;

import org.moxie.ply.Output;
import org.moxie.ply.PropertiesUtil;
import org.moxie.ply.dep.DependencyAtom;
import org.moxie.ply.dep.DependencyResolver;
import org.moxie.ply.dep.RepositoryAtom;

import java.io.File;
import java.util.*;
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
 * Dependencies are grouped by a context (i.e., test).  The default context is null.  Below ${context} represents this
 * dependency context.
 *
 * Dependency information is stored in a file called {@literal dependencies[.${context}].properties} and so the ply-context is
 * {@literal dependencies[.${context}]}.  The format of each property within the file is:
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
 * This script, run without arguments (except perhaps the context), will resolve all the dependencies listed
 * in {@literal dependencies[.${context}].properties} and store the values in file {@literal resolved-deps[.${context}].properties}
 * under the {@literal project.build.dir}.  This file will contain local file references (local to the {@literal localRepo})
 * for dependencies and transitive dependencies so that compilation and packaging may succeed.
 *
 * The dependency script's usage is:
 * <pre>dep [--usage] [--context] [add|remove|list|add-repo|remove-repo]</pre>
 * where {@literal --usage} prints the usage information.
 * The {@literal add} command takes an atom and adds it as a dependency for the supplied context, resolving it eagerly
 * from the known repos and failing if it cannot be resolved.
 * The {@literal remove} command takes an atom and removes it from the dependencies context, if it exists.
 * The {@literal list} command lists all dependencies for the context.
 * The {@literal add-repo} command takes a repository and adds it to the repositories.
 * The {@literal remove-repo} command removes the repository.
 * The context groups dependencies into logical units (i.e., test).  The default is null.  It is not applicable to repositories.
 * If nothing is passed to the script then dependency resolution is done for all dependencies against the known
 * repositories.
 */
public class DependencyManager {

    /**
     * Flavors of the context for file name prefix and pretty printing.
     */
    private static class Context {
        private final String name;
        private final String fileSuffix;
        private final String print;
        private Context(String contextName) {
            this.name = contextName;
            this.fileSuffix = (contextName.isEmpty() ? "" : "." + contextName);
            this.print = (contextName.isEmpty() ? "" : "^b^" + contextName + "^r^ ");
        }
        @Override public String toString() {
            return name;
        }
    }

    public static void main(String[] args) {
        if ((args == null) || (args.length > 0 && "--usage".equals(args[0]))) {
            usage();
            return;
        }
        Context context = new Context(((args.length > 0) && args[0].startsWith("--")) ? args[0].substring(2) : "");
        if (!context.name.isEmpty()) {
            args = removeContext(args);
        }

        if ((args.length > 1) && "add".equals(args[0])) {
            addDependency(args[1], context);
        } else if ((args.length > 1) && "remove".equals(args[0])) {
            removeDependency(args[1], context);
        } else if ((args.length == 1) && "list".equals(args[0])) {
            Map<String, String> dependencies = getDependenciesFromEnv(context);
            int size = dependencies.size();
            if (size > 0) {
                Output.print("Project ^b^%s^r^ has ^b^%d^r^ %sdependenc%s: ", System.getenv("project.name"), size,
                        context.print, (size == 1 ? "y" : "ies"));
                for (String key : dependencies.keySet()) {
                    Output.print("\t%s::%s", key, dependencies.get(key));
                }
            } else {
                Output.print("Project ^b^%s^r^ has no %sdependencies.", System.getenv("project.name"), context.print);
            }
        } else if ((args.length > 1) && "add-repo".equals(args[0])) {
            addRepository(args[1]);
        } else if ((args.length > 1) && "remove-repo".equals(args[0])) {
            removeRepository(args[1]);
        } else if (args.length == 0) {
            Map<String, String> dependencies = getDependenciesFromEnv(context);
            int size = dependencies.size();
            if (size > 0) {
                Output.print("Resolving ^b^%d^r^ %sdependenc%s for ^b^%s^r^.", size, context.print, (size == 1 ? "y" : "ies"),
                        System.getenv("project.name"));
                Properties dependencyFiles = resolveDependencies(dependencies);
                storeResolvedDependenciesFile(dependencyFiles, context);
            }
        } else {
            usage();
        }
    }

    private static void addDependency(String dependency, Context context) {
        AtomicReference<String> error = new AtomicReference<String>(null);
        DependencyAtom atom = DependencyAtom.parse(dependency, error);
        if (atom == null) {
            Output.print("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace::name::version[::artifactName]).",
                    dependency, error.get());
            System.exit(1);
        }
        Properties dependencies = loadDependenciesFile(context);
        if (dependencies.contains(atom.getPropertyName())) {
            Output.print("^info^ overriding %sdependency %s; was %s now is %s.", context.print, atom.getPropertyName(),
                    dependencies.getProperty(atom.getPropertyName()), atom.getPropertyValue());
        }
        dependencies.put(atom.getPropertyName(), atom.getPropertyValue());
        if (resolveDependency(atom)) {
            storeDependenciesFile(dependencies, context);
        } else {
            Output.print("^error^ unable to resolve %sdependency ^b^%s^r^. Ensure you are able to connect to the remote repositories.",
                    context.print, atom.toString());
            System.exit(1);
        }
    }

    private static void removeDependency(String dependency, Context context) {
        DependencyAtom atom = DependencyAtom.parse(dependency, null);
        if (atom == null) {
            // allow non-version specification.
            String[] split = dependency.split("::");
            if (split.length < 2) {
                Output.print("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace::name[::version::artifactName]).",
                        dependency, (split.length == 1 ? "name" : "namespace and name"));
                System.exit(1);
            }
            atom = new DependencyAtom(split[0], split[1], null);
        }
        if (System.getenv("dependencies" + context.fileSuffix + "." + atom.getPropertyName()) == null) {
            Output.print("^warn^ Could not find %sdependency; given %s::%s", context.print, atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            Properties dependencies = loadDependenciesFile(context);
            dependencies.remove(atom.getPropertyName());
            storeDependenciesFile(dependencies, context);
        }
    }

    private static void addRepository(String repository) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format repoUri[::type].", repository);
            System.exit(1);
        }
        try {
            atom.repositoryUri.toURL();
        } catch (Exception e) {
            Output.print("^error^ Given value ^b^%s^r^ is not a valid URL and so is not a repository.",
                    atom.getPropertyName());
            System.exit(1);
        }
        Properties repositories = loadRepositoriesFile();
        if (repositories.contains(atom.getPropertyName())) {
            Output.print("^info^ overriding repository %s; was %s now is %s.", atom.getPropertyName(),
                    repositories.getProperty(atom.getPropertyName()), atom.getPropertyValue());
        }
        repositories.put(atom.getPropertyName(), atom.getPropertyValue());
        storeRepositoriesFile(repositories);
    }

    private static void removeRepository(String repository) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format repoUri[::type].", repository);
            System.exit(1);
        }

        if (System.getenv("repositories." + atom.getPropertyName()) == null) {
            Output.print("^warn^ Repository not found; given %s::%s", atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            Properties repositories = loadRepositoriesFile();
            repositories.remove(atom.getPropertyName());
            storeRepositoriesFile(repositories);
        }
    }

    private static List<RepositoryAtom> createRepositoryList() {
        RepositoryAtom localRepo = RepositoryAtom.parse(System.getenv("depmngr.localRepo"));
        if (localRepo == null) {
            Output.print("^error^ Local repository not defined.  Set 'localRepo' property in context 'depmngr'");
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
                    Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
                } else {
                    repositoryAtoms.add(repo);
                }
            }
        }
        return repositoryAtoms;
    }

    private static Properties resolveDependencies(Map<String, String> dependencies) {
        List<DependencyAtom> dependencyAtoms = new ArrayList<DependencyAtom>(dependencies.size());
        AtomicReference<String> error = new AtomicReference<String>();
        for (String dependencyKey : dependencies.keySet()) {
            error.set(null);
            String dependencyValue = dependencies.get(dependencyKey);
            DependencyAtom dependencyAtom = DependencyAtom.parse(dependencyKey + "::" + dependencyValue, error);
            if (dependencyAtom == null) {
                Output.print("^warn^ Invalid dependency %s::%s; missing %s", dependencyKey, dependencyValue,
                        error.get());
                continue;
            }
            dependencyAtoms.add(dependencyAtom);
        }

        return DependencyResolver.resolveDependencies(dependencyAtoms, createRepositoryList());
    }

    private static boolean resolveDependency(DependencyAtom dependencyAtom) {
        return DependencyResolver.resolveDependency(dependencyAtom, createRepositoryList(),
                new Properties());
    }

    private static Map<String, String> getDependenciesFromEnv(Context context) {
        Map<String, String> dependencies = new HashMap<String, String>();
        for (String environmentKey : System.getenv().keySet()) {
            if (environmentKey.startsWith("dependencies" + context.fileSuffix + ".")) {
                String dependencyKey = environmentKey.replace("dependencies" + context.fileSuffix + ".", "");
                String dependencyValue = System.getenv(environmentKey);
                dependencies.put(dependencyKey, dependencyValue);
            }
        }
        return dependencies;
    }

    private static Properties loadDependenciesFile(Context context) {
        String localDir = System.getenv("ply.project.dir");
        String loadPath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies" + context.fileSuffix + ".properties";
        return PropertiesUtil.load(loadPath, true);
    }

    private static Properties loadRepositoriesFile() {
        String localDir = System.getenv("ply.project.dir");
        String loadPath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties";
        return PropertiesUtil.load(loadPath, true);
    }

    private static void storeDependenciesFile(Properties dependencies, Context context) {
        String localDir = System.getenv("ply.project.dir");
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                            File.separator + "dependencies" + context.fileSuffix + ".properties";
        if (!PropertiesUtil.store(dependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeRepositoriesFile(Properties repositories) {
        String localDir = System.getenv("ply.project.dir");
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties";
        if (!PropertiesUtil.store(repositories, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeResolvedDependenciesFile(Properties resolvedDependencies, Context context) {
        String buildDirPath = System.getenv("project.build.dir");
        String storePath = buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + "resolved-deps" + context.fileSuffix + ".properties";
        if (!PropertiesUtil.store(resolvedDependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static String[] removeContext(String[] args) {
        String[] newArgs = new String[args.length - 1];
        System.arraycopy(args, 1, newArgs, 0, args.length - 1);
        return newArgs;
    }

    private static void usage() {
        Output.print("dep [--usage] [--context] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^add <dep-atom>^r^\t: adds dep-atom to the list of dependencies (within context) (or replacing the version if it already exists).");
        Output.print("    ^b^remove <dep-atom>^r^\t: removes dep-atom from the list of dependencies (within context).");
        Output.print("    ^b^list^r^\t\t\t: list all dependencies (within context).");
        Output.print("    ^b^add-repo <rep-atom>^r^\t: adds rep-atom to the list of repositories.");
        Output.print("    ^b^remove-repo <rep-atom>^r^: removes rep-atom from the list of repositories.");
        Output.print("  ^b^dep-atom^r^ is namespace::name::version[::artifactName] (artifactName is optional and defaults to name-version.jar).");
        Output.print("  ^b^rep-atom^r^ is repoURI[::type] (type is optional and defaults to ply, must be either ply or maven).");
        Output.print("  if no command is passed then dependency resolution is done for all dependencies against the known repositories.");
        Output.print("  Dependencies can be grouped by ^b^context^r^ (i.e. test).  The default context is null.");
    }

}
