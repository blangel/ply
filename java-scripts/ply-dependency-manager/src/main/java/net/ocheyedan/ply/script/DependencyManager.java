package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.dep.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/29/11
 * Time: 9:41 AM
 *
 * The default dependency manager for the ply build system.
 *
 * The property file used to configure this script is {@literal depmngr.properties} and so the context is {@literal depmngr}.
 * The following properties exist:
 * localRepo=string [[default=${PLY_HOME}/repo]] (this is the local repository where remote dependencies will
 *           be stored.  Note, multiple local-filesystem repositories may exist within {@literal repositories.properties}
 *           but only this, the {@literal localRepo}, will be used to store remote repositories' downloads.  The format
 *           is [type:]repoUri, see below for description of this format).
 *
 * Dependency information is stored in a file called {@literal dependencies[.scope].properties} and so the context is
 * {@literal dependencies[.scope]}.  The format of each property within the file is:
 * namespace:name=version:artifactName
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
 * {@literal org.apache.commons:commons-io=1.3.2:} then the dependency would be resolved by assuming the default
 * artifactName (which would be {@literal commons-io-1.3.2.jar}) and creating a link to the artifact.  If the type
 * of the repository were null or {@literal ply} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org.apache.commons/1.3.2/commons-io-1.3.2.jar}.
 * If the type were {@literal maven} the artifact would be:
 * {@literal http://repo1.maven.org/maven2/org/apache/commons/1.3.2/commons-io-1.3.2.jar}.
 * The difference between the two is that with the {@literal maven} type the dependency's {@literal namespace}'s periods
 * are resolved to forward slashes as is convention in the {@literal Maven} build system.
 *
 * This script, run without arguments will resolve all the dependencies listed in
 * {@literal dependencies[.scope].properties} and store the values in file {@literal resolved-deps[.scope].properties}
 * under the {@literal project.build.dir}.  This file will contain local file references (local to the {@literal localRepo})
 * for dependencies and transitive dependencies so that compilation and packaging may succeed.
 *
 * The dependency script's usage is:
 * <pre>dep [--usage] [add|remove|list|tree|add-repo|remove-repo]</pre>
 * where {@literal --usage} prints the usage information.
 * The {@literal add} command takes an atom and adds it as a dependency for the supplied scope, resolving it eagerly
 * from the known repos and failing if it cannot be resolved.
 * The {@literal remove} command takes an atom and removes it from the dependencies scope, if it exists.
 * The {@literal list} command lists all direct dependencies for the scope (transitive dependencies are not listed).
 * The {@literal tree} command lists all dependencies for the scope in a tree format (including transitive).
 * The {@literal add-repo} command takes a repository and adds it to the repositories.
 * The {@literal remove-repo} command removes the repository.
 *
 * If nothing is passed to the script then dependency resolution is done for all dependencies against the known
 * repositories.
 */
public class DependencyManager {

    public static void main(String[] args) {
        if ((args == null) || (args.length > 0 && "--usage".equals(args[0]))) {
            usage();
            return;
        }
        Scope scope = new Scope(Props.getValue("ply", "scope"));
        if ((args.length > 1) && "add".equals(args[0])) {
            addDependency(args[1], scope);
        } else if ((args.length > 1) && "remove".equals(args[0])) {
            removeDependency(args[1], scope);
        } else if ((args.length == 1) && "list".equals(args[0])) {
            Map<String, String> dependencies = getDependencies(scope);
            int size = dependencies.size();
            if (size > 0) {
                Output.print("Project ^b^%s^r^ has ^b^%d^r^ %sdependenc%s: ", Props.getValue("project", "name"), size,
                        scope.forPrint, (size == 1 ? "y" : "ies"));
                for (String key : dependencies.keySet()) {
                    Output.print("\t%s:%s", key, dependencies.get(key));
                }
            } else {
                Output.print("Project ^b^%s^r^ has no %sdependencies.", Props.getValue("project", "name"), scope.forPrint);
            }
        } else if ((args.length == 1) && "tree".equals(args[0])) {
            List<DependencyAtom> dependencies = Deps.parse(getDependencies(scope));
            if (dependencies.isEmpty()) {
                Output.print("Project ^b^%s^r^ has no %sdependencies.", Props.getValue("project", "name"), scope.forPrint);
            } else {
                DirectedAcyclicGraph<Dep> depGraph = Deps.getDependencyGraph(dependencies, createRepositoryList(null, null));
                int size = dependencies.size();
                Output.print("Project ^b^%s^r^ has ^b^%d^r^ direct %sdependenc%s: ", Props.getValue("project", "name"), size,
                        scope.forPrint, (size == 1 ? "y" : "ies"));
                printDependencyGraph(depGraph.getRootVertices(), "\u26AC ", new HashSet<Vertex<Dep>>());
            }
        } else if ((args.length > 1) && "add-repo".equals(args[0])) {
            addRepository(args[1]);
        } else if ((args.length > 1) && "remove-repo".equals(args[0])) {
            removeRepository(args[1]);
        } else if (args.length == 0) {
            Map<String, String> dependencies = getDependencies(scope);
            int size = dependencies.size();
            if (size > 0) {
                Output.print("Resolving ^b^%d^r^ %sdependenc%s for ^b^%s^r^.", size, scope.forPrint, (size == 1 ? "y" : "ies"),
                        Props.getValue("project", "name"));
                Properties dependencyFiles = resolveDependencies(dependencies);
                storeResolvedDependenciesFile(dependencyFiles, scope);
            }
        } else {
            usage();
        }
    }

    private static void addDependency(String dependency, Scope scope) {
        AtomicReference<String> error = new AtomicReference<String>(null);
        DependencyAtom atom = DependencyAtom.parse(dependency, error);
        if (atom == null) {
            Output.print("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace:name:version[:artifactName]).",
                    dependency, error.get());
            System.exit(1);
        }
        Properties dependencies = loadDependenciesFile(scope);
        if (dependencies.contains(atom.getPropertyName())) {
            Output.print("^info^ overriding %sdependency %s; was %s now is %s.", scope.forPrint, atom.getPropertyName(),
                    dependencies.getProperty(atom.getPropertyName()), atom.getPropertyValue());
        }
        dependencies.put(atom.getPropertyName(), atom.getPropertyValue());
        List<DependencyAtom> dependencyAtoms = Deps.parse(dependencies);
        RepositoryRegistry repositoryRegistry = createRepositoryList(Deps.getProjectDep(), dependencyAtoms);
        if (Deps.getDependencyGraph(dependencyAtoms, repositoryRegistry) != null) { // getDependencyGraph returns => ok
            storeDependenciesFile(dependencies, scope);
        }
    }

    private static void removeDependency(String dependency, Scope scope) {
        DependencyAtom atom = DependencyAtom.parse(dependency, null);
        if (atom == null) {
            // allow non-version specification.
            String[] split = dependency.split(":");
            if (split.length < 2) {
                Output.print("^error^ Dependency ^b^%s^r^ missing ^b^%s^r^ (format namespace:name[:version:artifactName]).",
                        dependency, (split.length == 1 ? "name" : "namespace and name"));
                System.exit(1);
            }
            atom = new DependencyAtom(split[0], split[1], null);
        }
        if (Props.get("dependencies", atom.getPropertyName()) == null) {
            Output.print("^warn^ Could not find %sdependency; given %s:%s", scope.forPrint, atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            Properties dependencies = loadDependenciesFile(scope);
            dependencies.remove(atom.getPropertyName());
            storeDependenciesFile(dependencies, scope);
        }
    }

    private static void addRepository(String repository) {
        RepositoryAtom atom = RepositoryAtom.parse(repository);
        if (atom == null) {
            Output.print("^error^ Repository %s not of format [type:]repoUri.", repository);
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
            Output.print("^error^ Repository %s not of format [type:]repoUri.", repository);
            System.exit(1);
        }

        if (Props.get("repositories", atom.getPropertyName()) == null) {
            Output.print("^warn^ Repository not found; given %s:%s", atom.getPropertyValue(),
                    atom.getPropertyName());
        } else {
            Properties repositories = loadRepositoriesFile();
            repositories.remove(atom.getPropertyName());
            storeRepositoriesFile(repositories);
        }
    }

    private static RepositoryRegistry createRepositoryList(DependencyAtom dependencyAtom, List<DependencyAtom> dependencyAtoms) {
        RepositoryAtom localRepo = RepositoryAtom.parse(Props.getValue("depmngr", "localRepo"));
        if (localRepo == null) {
            Output.print("^error^ Local repository not defined.  Set 'localRepo' property in context 'depmngr'");
            System.exit(1);
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        Map<String, Prop> repositories = Props.getProps("repositories");
        for (String repoUri : repositories.keySet()) {
            if (localRepo.getPropertyName().equals(repoUri)) {
                continue;
            }
            String repoType = repositories.get(repoUri).value;
            String repoAtom = repoType + ":" + repoUri;
            RepositoryAtom repo = RepositoryAtom.parse(repoAtom);
            if (repo == null) {
                Output.print("^warn^ Invalid repository declared %s, ignoring.", repoAtom);
            } else {
                repositoryAtoms.add(repo);
            }
        }
        Collections.sort(repositoryAtoms, RepositoryAtom.LOCAL_COMPARATOR);
        Map<DependencyAtom, List<DependencyAtom>> synthetic = null;
        if (dependencyAtom != null) {
            synthetic = new HashMap<DependencyAtom, List<DependencyAtom>>(1);
            synthetic.put(dependencyAtom, dependencyAtoms);
        }
        return new RepositoryRegistry(localRepo, repositoryAtoms, synthetic);
    }

    private static Properties resolveDependencies(Map<String, String> dependencies) {
        DependencyAtom self = Deps.getProjectDep();
        List<DependencyAtom> dependencyAtoms = Deps.parse(dependencies);
        DirectedAcyclicGraph<Dep> dependencyGraph = Deps.getDependencyGraph(dependencyAtoms,
                createRepositoryList(self, dependencyAtoms));
        return Deps.convertToResolvedPropertiesFile(dependencyGraph);
    }

    private static Map<String, String> getDependencies(Scope scope) {
        // note, for non-default scoped invocations this is redundant as we add a dependency to the
        // default scope itself (which via transitive deps will depend upon all the inherited deps anyway).
        // TODO - is this wrong? essentially saying transitive deps are first level deps for non-default scopes
        Map<String, Prop> scopedDependencies = Props.getProps("dependencies");
        Map<String, String> dependencies = new HashMap<String, String>();
        if (!scope.name.isEmpty()) {
            // add the project itself as this is not the default scope
            DependencyAtom self = DependencyAtom.parse(Props.getValue("project", "nonscoped.artifact.name"), null);
            if (self == null) {
                throw new AssertionError("Could not determine the project information.");
            }
            dependencies.put(self.namespace + ":" + self.name, self.version + ":" + self.getArtifactName());
        }
        if (scopedDependencies == null) {
            return dependencies;
        }
        for (Prop dependency : scopedDependencies.values()) {
            dependencies.put(dependency.name, dependency.value);
        }
        return dependencies;
    }

    private static Properties loadDependenciesFile(Scope scope) {
        String localDir = Props.getValue("ply", "project.dir");
        String loadPath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies" + scope.fileSuffix + ".properties";
        return PropertiesFileUtil.load(loadPath, true);
    }

    private static Properties loadRepositoriesFile() {
        String localDir = Props.getValue("ply", "project.dir");
        String loadPath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties";
        return PropertiesFileUtil.load(loadPath, true);
    }

    private static void storeDependenciesFile(Properties dependencies, Scope scope) {
        String localDir = Props.getValue("ply", "project.dir");
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies" + scope.fileSuffix + ".properties";
        if (!PropertiesFileUtil.store(dependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeRepositoriesFile(Properties repositories) {
        String localDir = Props.getValue("ply", "project.dir");
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "repositories.properties";
        if (!PropertiesFileUtil.store(repositories, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeResolvedDependenciesFile(Properties resolvedDependencies, Scope scope) {
        String buildDirPath = Props.getValue("project", "build.dir");
        String storePath = buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + "resolved-deps" + scope.fileSuffix + ".properties";
        if (!PropertiesFileUtil.store(resolvedDependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static void printDependencyGraph(List<Vertex<Dep>> vertices, String indent, Set<Vertex<Dep>> encountered) {
        if ((vertices == null) || vertices.isEmpty()) {
            return;
        }
        for (Vertex<Dep> vertex : vertices) {
            boolean enc = encountered.contains(vertex);
            if (!enc) {
                encountered.add(vertex);
            }
            String name = vertex.getValue().dependencyAtom.getPropertyName();
            String version = vertex.getValue().dependencyAtom.getPropertyValue();
            Output.print("%s^b^%s:%s^r^%s", indent, name, version, (enc ? " (already printed)" : ""));
            if (!enc) {
                printDependencyGraph(vertex.getChildren(), String.format("  \u2937 %s", indent), encountered);
            }
        }
    }

    private static void usage() {
        Output.print("dep [--usage] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^add <dep-atom>^r^\t: adds dep-atom to the list of dependencies (within scope) (or replacing the version if it already exists).");
        Output.print("    ^b^remove <dep-atom>^r^\t: removes dep-atom from the list of dependencies (within scope).");
        Output.print("    ^b^list^r^\t\t\t: list all direct dependencies (within scope excluding transitive dependencies).");
        Output.print("    ^b^tree^r^\t\t\t: print all dependencies in a tree view (within scope including transitive dependencies).");
        Output.print("    ^b^add-repo <rep-atom>^r^\t: adds rep-atom to the list of repositories.");
        Output.print("    ^b^remove-repo <rep-atom>^r^: removes rep-atom from the list of repositories.");
        Output.print("  ^b^dep-atom^r^ is namespace:name:version[:artifactName] (artifactName is optional and defaults to name-version.jar).");
        Output.print("  ^b^rep-atom^r^ is [type:]repoURI (type is optional and defaults to ply, must be either ply or maven).");
        Output.print("  if no command is passed then dependency resolution is done for all dependencies against the known repositories.");
        Output.print("  Dependencies can be grouped by ^b^scope^r^ (i.e. test).  The default scope is null.");
    }

}
