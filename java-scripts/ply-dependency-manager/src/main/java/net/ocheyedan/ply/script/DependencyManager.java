package net.ocheyedan.ply.script;

import net.ocheyedan.ply.*;
import net.ocheyedan.ply.dep.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.props.*;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static net.ocheyedan.ply.props.PropFile.Prop;

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
 * <pre>dep [--usage] [add|rm|exclude|exclude-min|list|tree]</pre>
 * where {@literal --usage} prints the usage information.
 * The {@literal add} command takes an atom and adds it as a dependency for the supplied scope, resolving it eagerly
 * from the known repositories and failing if it cannot be resolved.
 * The {@literal rm} command takes an atom and removes it from the dependencies scope, if it exists.
 * The {@literal exclude} command takes an atom and excludes it from the dependency graph for the local project.
 * The {@literal exclude-min} command for any conflicting dependency atoms, attempts to exclude the minimum versions
 * The {@literal list} command lists all direct dependencies for the scope (transitive dependencies are not listed).
 * The {@literal tree} command lists all dependencies for the scope in a tree format (including transitive).
 *
 * If nothing is passed to the script then dependency resolution is done for all dependencies against the known
 * repositories.
 */
public class DependencyManager {

    private static final String TRANSIENT_PRINT = " ^black^[transient]^r^";

    public static void main(String[] args) {
        if ((args == null) || (args.length > 0 && "--usage".equals(args[0]))) {
            usage();
            return;
        }
        Scope scope = Scope.named(Props.get("scope", Context.named("ply")).value());
        Context projectContext = Context.named("project");
        if ((args.length > 1) && "add".equals(args[0])) {
            addDependency(args[1], scope);
        } else if ((args.length > 1) && "rm".equals(args[0])) {
            removeDependency(args[1], scope);
        } else if ((args.length > 1) && "exclude".equals(args[0])) {
            excludeDependency(args[1], scope);
        } else if ((args.length == 1) && "exclude-min".equals(args[0])) {
            excludeMinimum(scope);
        } else if ((args.length == 1) && "list".equals(args[0])) {
            PropFile dependencies = getDependencies(scope);
            int size = dependencies.size();
            if (size > 0) {
                Output.print("Project ^b^%s^r^ has ^b^%d^r^ %sdependenc%s: ", Props.get("name", projectContext).value(), size,
                        scope.getPrettyPrint(), (size == 1 ? "y" : "ies"));
                List<PropFile.Prop> sorted = collect(dependencies.props(), size);
                Collections.sort(sorted);
                for (PropFile.Prop dep : sorted) {
                    String value = dep.value();
                    boolean transientDep = DependencyAtom.isTransient(value);
                    if (transientDep) {
                        value = DependencyAtom.stripTransient(value);
                    }
                    Output.print("  %s:%s%s", dep.name, value, (transientDep ? TRANSIENT_PRINT : ""));
                }
            } else {
                Output.print("Project ^b^%s^r^ has no %sdependencies.", Props.get("name", projectContext).value(), scope.getPrettyPrint());
            }
        } else if ((args.length == 1) && "tree".equals(args[0])) {
            List<DependencyAtom> dependencies = Deps.parse(getDependencies(scope), null);
            if (dependencies.isEmpty()) {
                Output.print("Project ^b^%s^r^ has no %sdependencies.", Props.get("name", projectContext).value(), scope.getPrettyPrint());
            } else {
                Set<DependencyAtom> exclusions = new HashSet<DependencyAtom>(Deps.parseExclusions(getExclusions(scope), null));
                DirectedAcyclicGraph<Dep> depGraph = Deps.getDependencyGraph(dependencies, exclusions, createRepositoryList(null, null));
                int size = dependencies.size();
                int graphSize = depGraph.getRootVertices().size();
                if (graphSize > size) {
                    throw new AssertionError("Dependency graph's root-vertices should not be greater than the specified dependencies.");
                }
                String sizeExplanation = (size != graphSize) ?
                        String.format(" [ actually %d; %d of which %s pulled in transitively ]", size, (size - graphSize), (size - graphSize) > 1 ? "are" : "is") : "";
                Output.print("Project ^b^%s^r^ has ^b^%d^r^ direct %sdependenc%s%s: ", Props.get("name", projectContext).value(), graphSize,
                        scope.getPrettyPrint(), (size == 1 ? "y" : "ies"), sizeExplanation);
                printDependencyGraph(depGraph.getRootVertices(), "" /*String.format("%s ", PlyUtil.isUnicodeSupported() ? "\u26AC" : "+")*/, 0, new HashSet<Vertex<Dep>>());
            }
        } else if ((args.length > 1) && "resolve-classifiers".equals(args[0])) {
            String[] classifiers = args[1].split(",");
            PropFile dependencies = getDependencies(scope);
            PropFile exclusions = getExclusions(scope);
            for (String classifier : classifiers) {
                resolveDependencies(dependencies, exclusions, classifier, false);
            }
        } else if (args.length == 0) {
            PropFile dependencies = getDependencies(scope);
            PropFile exclusions = getExclusions(scope);
            int size = dependencies.size();
            int exclusionsSize = getExclusionsSize(exclusions);
            if (size > 0) {
                String exclusionsDescription = "";
                if (exclusionsSize > 0) {
                    exclusionsDescription = String.format(" (with ^b^%d^r^ exclusion%s)", exclusionsSize, (exclusionsSize == 1 ? "" : "s"));
                }
                Output.print("Resolving ^b^%d^r^ %sdependenc%s for ^b^%s^r^%s.", size, scope.getPrettyPrint(), (size == 1 ? "y" : "ies"),
                        Props.get("name", projectContext).value(), exclusionsDescription);
                PropFile dependencyFiles = resolveDependencies(dependencies, exclusions, null, true);
                storeResolvedDependenciesFile(dependencyFiles, scope);
            }
        } else {
            usage();
        }
    }

    private static List<PropFile.Prop> collect(Iterable<Prop> deps, int expectedSize) {
        List<Prop> props = new ArrayList<Prop>(expectedSize);
        for (Prop dep : deps) {
            props.add(dep);
        }
        return props;
    }

    private static int getExclusionsSize(PropFile exclusions) {
        int size = exclusions.size();
        for (Prop exclusion : exclusions.props()) {
            size += countMatches(exclusion.value(), ' ');
        }
        return size;
    }

    private static int countMatches(String value, char character) {
        if ((value == null) || value.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = value.indexOf(character, index)) != -1) {
            count++;
            index += 1;
        }
        return count;
    }

    private static void addDependency(String dependency, Scope scope) {
        AtomicReference<String> error = new AtomicReference<String>(null);
        DependencyAtom atom = DependencyAtom.parse(dependency, error);
        if (atom == null) {
            Output.print("^error^ Dependency ^b^%s^r^ ^b^%s^r^ (format namespace:name:version[:artifactName]).",
                    dependency, error.get());
            throw new SystemExit(1);
        }
        PropFile dependencies = loadDependenciesFile(scope);
        if (dependencies.contains(atom.getPropertyName())) {
            PropFile.Prop existingDep = dependencies.get(atom.getPropertyName());
            DependencyAtom existing = Deps.parse(existingDep);
            if (existing == null) {
                Output.print("^error^ Could not parse dependency atom, %s=%s, listed in dependencies%s.properties.",
                        existingDep.name, existingDep.value(), scope.getFileSuffix());
            } else if (existing.equals(atom)) {
                Output.print("Project already depends upon %s%s, skipping.", atom.toString(), !Scope.Default.equals(scope) ?
                        String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
                return;
            } else {
                Output.print("^warn^ overriding %sdependency %s; was %s now is %s.", scope.getPrettyPrint(), atom.getPropertyName(),
                        dependencies.get(atom.getPropertyName()).value(), atom.getPropertyValue());
                Prop existingProp = dependencies.remove(atom.getPropertyName());
                Deps.addChangedDependency(scope, Deps.parse(existingProp));
            }
        }
        dependencies.add(atom.getPropertyName(), atom.getPropertyValue());
        final List<DependencyAtom> dependencyAtoms = Deps.parse(dependencies, null);
        PropFile exclusions = getExclusions(scope);
        final Set<DependencyAtom> exclusionAtoms = new HashSet<DependencyAtom>(Deps.parseExclusions(exclusions, null));
        final RepositoryRegistry repositoryRegistry = createRepositoryList(Deps.getProjectDep(), dependencyAtoms);
        DirectedAcyclicGraph<Dep> resolvedDeps = invokeWithSlowResolutionThread(new Callable<DirectedAcyclicGraph<Dep>>() {
            @Override public DirectedAcyclicGraph<Dep> call() throws Exception {
                return Deps.getDependencyGraph(dependencyAtoms, exclusionAtoms, repositoryRegistry);
            }
        }, String.format("^b^Hang tight,^r^ %s has a lot of dependencies. ^b^Ply^r^'s downloading them...", dependency));
        if (resolvedDeps != null) { // getDependencyGraph returns => ok
            storeDependenciesFile(dependencies, scope);
        }
        Output.print("Added dependency %s%s", atom.toString(), !Scope.Default.equals(scope) ?
                String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
    }

    private static void removeDependency(String dependency, Scope scope) {
        DependencyAtom atom = DependencyAtom.parse(dependency, null);
        if (atom == null) {
            // allow non-version specification.
            String[] split = dependency.split(":");
            if (split.length < 2) {
                Output.print("^error^ Dependency ^b^%s^r^ ^b^%s^r^ (format namespace:name[:version:artifactName]).",
                        dependency, (split.length == 1 ? "name" : "namespace and name"));
                System.exit(1);
            }
            atom = new DependencyAtom(split[0], split[1], null);
        }
        PropFile dependencies = loadDependenciesFile(scope);
        Prop existing;
        if (Props.get(atom.getPropertyName(), Context.named("dependencies"), scope).value().isEmpty()
                || ((existing = dependencies.remove(atom.getPropertyName())) == null)) {
            Output.print("^warn^ Could not find %sdependency; given %s:%s", scope.getPrettyPrint(), atom.getPropertyName(),
                    atom.getPropertyValue());
        } else {
            storeDependenciesFile(dependencies, scope);
            Deps.addChangedDependency(scope, Deps.parse(existing));
            Output.print("Removed dependency %s%s", atom.toString(), !Scope.Default.equals(scope) ?
                String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
        }
    }

    private static void excludeDependency(String dependency, Scope scope) {
        DependencyAtom atom = DependencyAtom.parse(dependency, null);
        if (atom == null) {
            // allow non-version specification.
            String[] split = dependency.split(":");
            if (split.length < 2) {
                Output.print("^error^ Dependency ^b^%s^r^ ^b^%s^r^ (format namespace:name[:version:artifactName]).",
                        dependency, (split.length == 1 ? "name" : "namespace and name"));
                System.exit(1);
            }
            atom = new DependencyAtom(split[0], split[1], null);
        }
        PropFile exclusions = loadExclusionsFile(scope);
        exclude(atom, exclusions);
        storeExclusionsFile(exclusions, scope);
        Output.print("Excluded dependency %s%s", atom.toString(), !Scope.Default.equals(scope) ?
            String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
    }

    private static void excludeMinimum(Scope scope) {
        PropFile exclusions = getExclusions(scope);
        int excluded = 0, totalExclusions = 0;
        while ((excluded = excludeMinimumForCurrentExclusions(scope, exclusions)) != 0) {
            exclusions = loadExclusionsFile(scope);
            totalExclusions += excluded;
        }
        Output.print("Excluded %d dependencies%s", totalExclusions, !Scope.Default.equals(scope) ?
                String.format(" (in scope %s)", scope.getPrettyPrint()) : "");
    }

    private static int excludeMinimumForCurrentExclusions(Scope scope, PropFile exclusions) {
        PropFile dependencies = getDependencies(scope);
        final Set<DependencyAtom> foundExclusions = new HashSet<DependencyAtom>();
        resolveDependenciesImmediately(dependencies, exclusions, null, false, new ConflictingVersionVisitor() {
            @Override public void visit(Dep diffVersionDep, Dep resolvedDep, Vertex<Dep> parentVertex, DependencyAtom dependencyAtom, Graph<Dep> graph) {
                Vertex<Dep> diffVersionParent = graph.getVertex(diffVersionDep).getAnyParent();
                String diffVersionPath = Deps.getPathAsString(diffVersionParent, diffVersionDep.dependencyAtom);
                String path = Deps.getPathAsString(parentVertex, dependencyAtom);
                DependencyAtom lower = Deps.getMinimumVersion(diffVersionDep.dependencyAtom, resolvedDep.dependencyAtom);
                if (lower == null) {
                    Output.print("^warn^ Could not determine lower of the two versions for ^b^%s^r^ [ ^yellow^%s^r^ ] and [ ^yellow^%s^r^ ].", resolvedDep.toString(), diffVersionDep.dependencyAtom.getPropertyValue(), resolvedDep.dependencyAtom.getPropertyValue());
                    Output.print("^warn^   ^b^%s^r^ => %s", diffVersionDep.dependencyAtom.getPropertyValue(), (diffVersionPath == null ? "<direct dependency>" : diffVersionPath));
                    Output.print("^warn^   ^b^%s^r^ => %s", resolvedDep.dependencyAtom.getPropertyValue(), (path == null ? "<direct dependency>" : path));
                    Output.print("^warn^ You can manually resolve this by excluding one of these versions from your project's dependency graph: ^b^ply dep exclude^r^ [ ^b^%s^r^ | ^b^%s^r^ ]", diffVersionDep.toVersionString(), resolvedDep.toVersionString());
                    return;
                }
                if (((lower == resolvedDep.dependencyAtom) && (path == null))
                        || ((lower == diffVersionDep.dependencyAtom) && (diffVersionPath == null))) {
                    Output.print("^warn^ Directly depending upon a lower version than found elsewhere in the dependency graph for ^b^%s^r^ [ ^yellow^%s^r^ ] and [ ^yellow^%s^r^ ].", resolvedDep.toString(), diffVersionDep.dependencyAtom.getPropertyValue(), resolvedDep.dependencyAtom.getPropertyValue());
                    Output.print("^warn^   ^b^%s^r^ => %s", diffVersionDep.dependencyAtom.getPropertyValue(), (diffVersionPath == null ? "<direct dependency>" : diffVersionPath));
                    Output.print("^warn^   ^b^%s^r^ => %s", resolvedDep.dependencyAtom.getPropertyValue(), (path == null ? "<direct dependency>" : path));
                    Output.print("^warn^ Either remove the direct dependency or manually exclude the higher version.");
                    return;
                }
                Output.print("^info^ Excluding ^b^%s^r^ version ^b^%s^r^ (as it is less than version ^b^%s^r^)", lower.getPropertyName(), lower.getPropertyValue(),
                        (lower == diffVersionDep.dependencyAtom ? resolvedDep.dependencyAtom.getPropertyValue() : diffVersionDep.dependencyAtom.getPropertyValue()));
                foundExclusions.add(lower);
            }
        });
        PropFile exclusionsPropFile = loadExclusionsFile(scope);
        for (DependencyAtom foundExclusion : foundExclusions) {
            exclude(foundExclusion, exclusionsPropFile);
        }
        storeExclusionsFile(exclusionsPropFile, scope);
        return foundExclusions.size();
    }

    private static void exclude(DependencyAtom atom, PropFile exclusions) {
        if (exclusions.contains(atom.getPropertyName())) {
            String versions = String.format("%s %s", exclusions.get(atom.getPropertyName()).value(), atom.getPropertyValue());
            exclusions.set(atom.getPropertyName(), versions);
        } else {
            exclusions.add(atom.getPropertyName(), atom.getPropertyValue());
        }
    }

    private static RepositoryRegistry createRepositoryList(DependencyAtom dependencyAtom, List<DependencyAtom> dependencyAtoms) {
        try {
            return Repos.createRepositoryRegistry(PlyUtil.LOCAL_CONFIG_DIR, Props.getScope(), dependencyAtom, dependencyAtoms);
        } catch (SystemExit se) {
            System.exit(se.exitCode);
            throw new AssertionError("Not reachable");
        }
    }

    private static PropFile resolveDependencies(final PropFile dependencies, final PropFile exclusions, final String classifier, final boolean failMissingDependency) {
        return invokeWithSlowResolutionThread(new Callable<PropFile>() {
            @Override public PropFile call() throws Exception {
                return resolveDependenciesImmediately(dependencies, exclusions, classifier, failMissingDependency, null);
            }
        }, "^b^Hang tight,^r^ your project needs a lot of dependencies. ^b^Ply^r^'s downloading them...");
    }

    private static PropFile resolveDependenciesImmediately(PropFile dependencies, PropFile exclusions,
                                                           String classifier, boolean failMissingDependency,
                                                           ConflictingVersionVisitor conflictingVersionVisitor) {
        DependencyAtom self = Deps.getProjectDep();
        List<DependencyAtom> dependencyAtoms = Deps.parse(dependencies, classifier);
        Set<DependencyAtom> exclusionAtoms = new HashSet<DependencyAtom>(Deps.parseExclusions(exclusions, classifier));
        RepositoryRegistry repositoryRegistry = createRepositoryList(self, dependencyAtoms);
        DirectedAcyclicGraph<Dep> dependencyGraph;
        if (conflictingVersionVisitor != null) {
            dependencyGraph = Deps.getDependencyGraph(dependencyAtoms, exclusionAtoms, repositoryRegistry, classifier, failMissingDependency, conflictingVersionVisitor);
        } else {
            dependencyGraph = Deps.getDependencyGraph(dependencyAtoms, exclusionAtoms, repositoryRegistry, classifier, failMissingDependency);
        }
        return Deps.convertToResolvedPropertiesFile(dependencyGraph);
    }

    private static <T> T invokeWithSlowResolutionThread(Callable<T> callable, String message) {
        // if the project hasn't already resolved these dependencies locally and is not running with 'info' logging
        // it appears that ply has hung if downloading lots of dependencies...print out a warning if not running
        // in 'info' logging and dependency resolution takes longer than 2 seconds.
        try {
            return SlowTaskThread.<T>after(2000).warn(message).onlyIfNotLoggingInfo().whenDoing(callable).start();
        } catch (Exception e) {
            Output.print(e);
            throw new AssertionError(e);
        }
    }

    private static PropFile getDependencies(Scope scope) {
        PropFileChain scopedDependencyChain = Props.get(Context.named("dependencies"), scope);
        PropFile scopedDependencies = new PropFile(Context.named("dependencies"), scope, PropFile.Loc.Local);
        boolean dependUponSelf = Boolean.parseBoolean(Props.get("depend.upon.self", Context.named("project"), scope).value());
        if (dependUponSelf) {
            // add the project itself as this is not the default scope
            DependencyAtom self = DependencyAtom.parse(Props.get("nonscoped.artifact.name", Context.named("project")).value(), null);
            if (self == null) {
                throw new AssertionError("Could not determine the project information.");
            }
            scopedDependencies.add(self.namespace + ":" + self.name, self.version + ":" + self.getArtifactName());
        }
        for (Prop dependency : scopedDependencyChain.props()) {
            scopedDependencies.add(dependency.name, dependency.value());
        }
        return scopedDependencies;
    }

    private static PropFile getExclusions(Scope scope) {
        PropFileChain exclusions = Props.get(Context.named("exclusions"), scope);
        PropFile scopedExclusions = new PropFile(Context.named("exclusions"), scope, PropFile.Loc.Local);
        for (Prop exclusion : exclusions.props()) {
            if (scopedExclusions.contains(exclusion.name)) {
                String versions = String.format("%s %s", scopedExclusions.get(exclusion.name).value(), exclusion.value());
                scopedExclusions.set(exclusion.name, versions);
            } else {
                scopedExclusions.add(exclusion.name, exclusion.value());
            }
        }
        return scopedExclusions;
    }

    private static PropFile loadDependenciesFile(Scope scope) {
        return loadFile(scope, "dependencies", true);
    }

    private static PropFile loadExclusionsFile(Scope scope) {
        return loadFile(scope, "exclusions", false);
    }

    private static PropFile loadFile(Scope scope, String fileName, boolean create) {
        String localDir = Props.get("project.dir", Context.named("ply")).value();
        String loadPath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + fileName + scope.getFileSuffix() + ".properties";
        return PropFiles.load(loadPath, create, false);
    }

    private static void storeDependenciesFile(PropFile dependencies, Scope scope) {
        String localDir = Props.get("project.dir", Context.named("ply")).value();
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "dependencies" + scope.getFileSuffix() + ".properties";
        if (!PropFiles.store(dependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeExclusionsFile(PropFile exclusions, Scope scope) {
        String localDir = Props.get("project.dir", Context.named("ply")).value();
        String storePath = localDir + (localDir.endsWith(File.separator) ? "" : File.separator) + "config" +
                File.separator + "exclusions" + scope.getFileSuffix() + ".properties";
        if (!PropFiles.store(exclusions, storePath, true)) {
            System.exit(1);
        }
    }

    private static void storeResolvedDependenciesFile(PropFile resolvedDependencies, Scope scope) {
        String storePath = getBuildDirStorePath("resolved-deps", scope);
        if (!PropFiles.store(resolvedDependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static String getBuildDirStorePath(String name, Scope scope) {
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        return buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + name + scope.getFileSuffix() + ".properties";
    }

    private static void printDependencyGraph(List<Vertex<Dep>> vertices, String indent, int depth, Set<Vertex<Dep>> encountered) {
        if ((vertices == null) || vertices.isEmpty()) {
            return;
        }
        for (Vertex<Dep> vertex : vertices) {
            int currentDepth = depth;
            boolean enc = encountered.contains(vertex);
            if (!enc) {
                encountered.add(vertex);
            }
            Dep dep = vertex.getValue();
            String namespace = dep.dependencyAtom.namespace;
            String name = dep.dependencyAtom.name;
            String version = dep.dependencyAtom.getPropertyValueWithoutTransient();
            Output.print("%s%s^grey^%s:^r^^b^%s:%s^r^%s%s", indent, getDepthSubscript(depth), namespace, name, version, (dep.dependencyAtom.transientDep ? TRANSIENT_PRINT : ""),
                    (enc ? " (already printed)" : ""));
            if (!enc && !dep.dependencyAtom.transientDep) {
                printDependencyGraph(vertex.getChildren(), String.format("  %s %s", PlyUtil.isUnicodeSupported() ? "\u2937" : "\\", indent),
                                     ++currentDepth, encountered);
            }
        }
    }

    private static String getDepthSubscript(int depth) {
        switch (depth) {
            case 0:
                return PlyUtil.isUnicodeSupported() ? "\u2080 " : "0 ";
            case 1:
                return PlyUtil.isUnicodeSupported() ? "\u2081 " : "1 ";
            case 2:
                return PlyUtil.isUnicodeSupported() ? "\u2082 " : "2 ";
            case 3:
                return PlyUtil.isUnicodeSupported() ? "\u2083 " : "3 ";
            case 4:
                return PlyUtil.isUnicodeSupported() ? "\u2084 " : "4 ";
            case 5:
                return PlyUtil.isUnicodeSupported() ? "\u2085 " : "5 ";
            case 6:
                return PlyUtil.isUnicodeSupported() ? "\u2086 " : "6 ";
            case 7:
                return PlyUtil.isUnicodeSupported() ? "\u2087 " : "7 ";
            case 8:
                return PlyUtil.isUnicodeSupported() ? "\u2088 " : "8 ";
            case 9:
                return PlyUtil.isUnicodeSupported() ? "\u2089 " : "9 ";
            default:
                return PlyUtil.isUnicodeSupported() ? "\u2089\u208A " : "9+ ";
        }
    }

    private static void usage() {
        Output.print("dep [--usage] <^b^command^r^>");
        Output.print("  where ^b^command^r^ is either:");
        Output.print("    ^b^add <dep-atom>^r^ : adds dep-atom to the list of dependencies (within scope) (or replacing the version if it already exists).");
        Output.print("    ^b^rm <dep-atom>^r^ : removes dep-atom from the list of dependencies (within scope).");
        Output.print("    ^b^exclude <dep-atom>^r^ : excludes dep-atom from the list of dependencies (within scope).");
        Output.print("    ^b^exclude-min^r^ : for any conflicting versions, excludes the minimum version (within scope).");
        Output.print("    ^b^list^r^ : list all direct dependencies (within scope excluding transitive dependencies).");
        Output.print("    ^b^tree^r^ : print all dependencies in a tree view (within scope including transitive dependencies).");
        Output.print("    ^b^resolve-classifiers <classifiers>^r^ : resolves dependencies with each of the (comma delimited) classifiers.");
        Output.print("  ^b^dep-atom^r^ is namespace:name:version[:artifactName] (artifactName is optional and defaults to name-version.jar).");
        Output.print("  if no command is passed then dependency resolution is done for all dependencies against the known repositories.");
        Output.print("  Dependencies can be grouped by ^b^scope^r^ (i.e. test).  The default scope is null.");
    }

}
