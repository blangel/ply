package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Graphs;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.mvn.MavenPom;
import net.ocheyedan.ply.mvn.MavenPomParser;
import net.ocheyedan.ply.props.*;

import java.io.File;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:41 PM
 *
 * Utility class to interact with {@link DependencyAtom} and {@link Dep} objects.
 */
public final class Deps {

    /**
     * Encapsulates a {@link DependencyAtom} object's paths to the local {@link RepositoryAtom}.
     */
    static class LocalPaths {

        final String localPath;
        final URL localUrl;
        final String localDirUrlPath;
        final String localDirPath;

        LocalPaths(String localPath, URL localUrl, String localDirUrlPath, String localDirPath) {
            this.localPath = localPath;
            this.localUrl = localUrl;
            this.localDirUrlPath = localDirUrlPath;
            this.localDirPath = localDirPath;
        }

        static LocalPaths get(DependencyAtom dependencyAtom, RepositoryAtom localRepo) {
            String localDirUrlPath = getDependencyDirectoryPathForRepo(dependencyAtom, localRepo);
            String localPath = getDependencyArtifactPathForRepo(dependencyAtom, localRepo);
            localPath = ensureProtocol(localPath);
            URL localUrl = getUrl(localPath);
            if (localUrl == null) {
                throw new AssertionError(String.format("The local path is not valid [ %s ]", localPath));
            }
            String localDirPath = FileUtil.stripFileUriPrefix(localDirUrlPath);
            return new LocalPaths(localPath, localUrl, localDirUrlPath, localDirPath);
        }

    }

    /**
     * Encapsulates state needed to fill a dependency graph.
     */
    private static class FillGraphState {

        /**
         * A mapping from {@link DependencyAtom} to {@link Dep} of already resolved dependencies within the graph
         */
        Map<DependencyAtom, Dep> resolved;

        /**
         * A mapping from {@link String} to {@link Dep} of already resolved dependencies where the key represents
         * the un-versioned dependency (to warn about conflicting versions within the graph).
         */
        Map<String, Set<Dep>> unversionedResolved;

        /**
         * Set of {@literal namespace:name} strings for which a conflicting version warning has already been printed.
         */
        Set<String> unversionedResolvedAlreadyWarned;

        private FillGraphState() {
            this.resolved = new ConcurrentHashMap<DependencyAtom, Dep>();
            this.unversionedResolved = new ConcurrentHashMap<String, Set<Dep>>();
            this.unversionedResolvedAlreadyWarned = new HashSet<String>();
        }
    }

    /**
     * @param dependencyAtoms the direct dependencies from which to create a dependency graph
     * @param exclusionAtoms the {@link DependencyAtom} to exclude when resolving transitive dependencies.
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @return a DAG {@link Graph<Dep>} implementation representing the resolved {@code dependencyAtoms} and its tree of
     *         transitive dependencies.
     */
    public static DirectedAcyclicGraph<Dep> getDependencyGraph(List<DependencyAtom> dependencyAtoms,
                                                               Set<DependencyAtom> exclusionAtoms,
                                                               RepositoryRegistry repositoryRegistry) {
        return getDependencyGraph(dependencyAtoms, exclusionAtoms, repositoryRegistry, null, true);
    }

    /**
     * @param dependencyAtoms the direct dependencies from which to create a dependency graph
     * @param exclusionAtoms the {@link DependencyAtom} to exclude when resolving transitive dependencies.
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @param classifier to use when resolving all transitive dependencies
     * @param failMissingDependency true to fail on missing dependencies; false to ignore and continue resolution
     * @return a DAG {@link Graph<Dep>} implementation representing the resolved {@code dependencyAtoms} and its tree of
     *         transitive dependencies.
     */
    public static DirectedAcyclicGraph<Dep> getDependencyGraph(List<DependencyAtom> dependencyAtoms,
                                                               Set<DependencyAtom> exclusionAtoms,
                                                               RepositoryRegistry repositoryRegistry,
                                                               String classifier,
                                                               boolean failMissingDependency) {
        DirectedAcyclicGraph<Dep> dependencyDAG = new DirectedAcyclicGraph<Dep>();
        Set<String> alreadyPrinted = new HashSet<String>((exclusionAtoms == null ? 16 : exclusionAtoms.size()));
        fillDependencyGraph(null, dependencyAtoms, exclusionAtoms, classifier, repositoryRegistry, dependencyDAG, new FillGraphState(),
                            alreadyPrinted, false, failMissingDependency);
        return dependencyDAG;
    }

    /**
     * For each resolved {@link Dep} object of {@code dependencyAtoms} a {@link Vertex<Dep>} will be created and added
     * to {@code graph}.  If {@code parentVertex} is not null, an edge will be added from {@code parentVertex} and
     * each resolved property.  If adding such an edge violates the acyclic nature of {@code graph} an error message
     * will be printed and this program will halt.
     * The {@code pomSufficient} exists as maven appears to be lax on transitive dependency's transitive dependency.
     * For instance, depending upon log4j:log4j:1.2.15 will fail b/c it depends upon javax.jms:jms:1.1 for which
     * there is no artifact in maven-central.  But if you depend upon org.apache.zookeeper:zookeeper:3.3.2
     * which depends upon log4j:log4j:1.2.15, resolution completes.  It will download the pom of
     * javax.jms:jms:1.1 but not require the packaged artifact (which is defaulted to jar) be present.  The
     * pom must be present.
     * @param parentVertex the parent of {@code dependencyAtoms}
     * @param dependencyAtoms the dependencies which to resolve and place into {@code graph}
     * @param exclusionAtoms the {@link DependencyAtom} to exclude when resolving transitive dependencies.
     * @param classifier to use when resolving transitive dependencies, or null
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @param graph to fill with the resolved {@link Dep} objects of {@code dependencyAtoms}.
     * @param state the {@link FillGraphState} used to track previously resolved dependencies, etc.
     * @param alreadyPrinted set of messages already printed
     * @param pomSufficient if true, then only the pom from a maven repository is necessary to have successfully
     *                      resolved the {@code dependencyAtom}.
     * @param failMissingDependency if true indicates that missing dependencies are treated as failures; false, to
     *                              ignore and continue resolution
     */
    private static void fillDependencyGraph(Vertex<Dep> parentVertex, List<DependencyAtom> dependencyAtoms,
                                            Set<DependencyAtom> exclusionAtoms, String classifier,
                                            RepositoryRegistry repositoryRegistry, DirectedAcyclicGraph<Dep> graph,
                                            FillGraphState state, Set<String> alreadyPrinted,
                                            boolean pomSufficient, boolean failMissingDependency) {
        if (repositoryRegistry.isEmpty()) {
            Output.print("^error^ No repositories found, cannot resolve dependencies.");
            SystemExit.exit(1);
        }
        for (DependencyAtom dependencyAtom : dependencyAtoms) {
            if ((parentVertex != null) && dependencyAtom.transientDep) {
                continue; // non-direct (transitive) transient dependencies should be skipped
            }
            // strip any classifier information for exclusion check
            DependencyAtom exclusionCheck = dependencyAtom.withoutClassifier();
            if ((parentVertex != null) && exclusionAtoms.contains(exclusionCheck)) {
                String key = String.format("exclusions:%s", dependencyAtom.toString());
                if (!alreadyPrinted.contains(key)) {
                    alreadyPrinted.add(key);
                    Output.print("^info^ Skipping excluded dependency ^b^%s^r^.", dependencyAtom.toString());
                }
                continue; // non-direct dependency listed in exclusions, skip
            } else if ((parentVertex == null) && exclusionAtoms.contains(dependencyAtom)) {
                Output.print("^error^ Direct dependency ^b^%s^r^ listed in exclusions, remove as dependency or as exclusion.",
                        dependencyAtom.toString());
                SystemExit.exit(1);
            }
            // pom is sufficient for resolution if this is a transient dependency
            Dep resolvedDep;
            try {
                if (state.resolved.containsKey(dependencyAtom)) {
                    resolvedDep = state.resolved.get(dependencyAtom);
                } else {
                    resolvedDep = resolveDependency(dependencyAtom, classifier, repositoryRegistry, (pomSufficient || dependencyAtom.transientDep),
                                                failMissingDependency);
                    if (resolvedDep != null) {
                        state.resolved.put(dependencyAtom, resolvedDep);
                    }
                    String key = (resolvedDep == null ? dependencyAtom.getPropertyName() : resolvedDep.toString());
                    Set<Dep> alreadyResolved = state.unversionedResolved.get(key);
                    if (alreadyResolved == null) {
                        alreadyResolved = new HashSet<Dep>(4, 1.0f);
                        state.unversionedResolved.put(key, alreadyResolved);
                    }
                    alreadyResolved.add(resolvedDep);
                }
                if ((resolvedDep == null) && !failMissingDependency) {
                    if (Output.isInfo()) {
                        Output.print("^info^ Could not resolve dependency ^b^%s^r^.", dependencyAtom.toString());
                        String path = getPathAsString(parentVertex, dependencyAtom);
                        if (path != null) {
                            Output.print("^info^ path to unresolved dependency [ %s ].", path);
                        }
                    }
                    continue;
                }
            } catch (Throwable t) {
                Output.print(t);
                resolvedDep = null; // allow the path to the dependency to be printed
            }
            if (resolvedDep == null) {
                String path = getPathAsString(parentVertex, dependencyAtom);
                if (path != null) {
                    Output.print("^error^ path to missing dependency [ %s ].", path);
                }
                SystemExit.exit(1);
            }
            Vertex<Dep> vertex = graph.addVertex(resolvedDep);
            if (parentVertex != null) {
                try {
                    graph.addEdge(parentVertex, vertex);
                } catch (Graph.CycleException gce) {
                    Output.print("^error^ circular dependency [ %s ].", getCycleAsString(gce));
                    SystemExit.exit(1);
                }
            }
            Set<Dep> resolved = state.unversionedResolved.get(resolvedDep.toString());
            if ((resolved != null)
                    && (resolved.size() > 1)
                    && !state.unversionedResolvedAlreadyWarned.contains(resolvedDep.toString())) {
                Dep diffVersionDep = null;
                for (Dep dep : resolved) {
                    if ((dep != null) && !dep.toVersionString().equals(resolvedDep.toVersionString())) {
                        diffVersionDep = dep;
                        break;
                    }
                }
                if (diffVersionDep != null) {
                    warnAboutMultipleVersions(diffVersionDep, resolvedDep, parentVertex, dependencyAtom, graph);
                    state.unversionedResolvedAlreadyWarned.add(resolvedDep.toString());
                }
            }
            if (!dependencyAtom.transientDep) { // direct transient dependencies are not recurred upon
                fillDependencyGraph(vertex, vertex.getValue().dependencies, exclusionAtoms, classifier, repositoryRegistry, graph, state, alreadyPrinted, true, failMissingDependency);
            }
        }
    }

    private static void warnAboutMultipleVersions(Dep diffVersionDep, Dep resolvedDep, Vertex<Dep> parentVertex,
                                                  DependencyAtom dependencyAtom, Graph<Dep> graph) {
        Vertex<Dep> diffVersionParent = graph.getVertex(diffVersionDep).getAnyParent();
        String diffVersionPath = getPathAsString(diffVersionParent, diffVersionDep.dependencyAtom);
        String path = getPathAsString(parentVertex, dependencyAtom);
        Output.print("^warn^ Dependency graph contains conflicting versions for ^b^%s^r^ [ ^yellow^%s^r^ ] and [ ^yellow^%s^r^ ].",
                resolvedDep.toString(), diffVersionDep.dependencyAtom.getPropertyValue(), resolvedDep.dependencyAtom.getPropertyValue());
        Output.print("^warn^   ^b^%s^r^ => %s", diffVersionDep.dependencyAtom.getPropertyValue(), (diffVersionPath == null ? "<direct dependency>" : diffVersionPath));
        Output.print("^warn^   ^b^%s^r^ => %s", resolvedDep.dependencyAtom.getPropertyValue(), (path == null ? "<direct dependency>" : path));
        Output.print("^warn^ You can resolve this warning by excluding one of these versions from your project's dependency graph: ^b^ply dep exclude^r^ [ ^b^%s^r^ | ^b^%s^r^ ]",
                diffVersionDep.toVersionString(), resolvedDep.toVersionString());
    }

    /**
     *@param gce the cycle exception to print
     *@return a string which includes the cycle and path information from {@code gce}
     */
    @SuppressWarnings("unchecked")
    private static String getCycleAsString(Graph.CycleException gce) {
        StringBuilder buffer = new StringBuilder();
        List<Vertex<?>> cycle = gce.getCycle();
        List<Vertex<?>> path = gce.getPath();
        for (int i = 0; i < (path.size() - 1); i++) {
            Vertex<Dep> vertex = (Vertex<Dep>) path.get(i);
            if (buffer.length() > 0) {
                buffer.append(" -> ");
            }
            buffer.append(vertex.getValue().toVersionString());
        }
        for (int i = 0; i < cycle.size(); i++) {
            if (buffer.length() > 0) {
                buffer.append(" -> ");
            }
            boolean decorate = (i == 0 ) || (i == (cycle.size() - 1));
            if (decorate) {
                buffer.append("^red^");
            }
            buffer.append(((Vertex<Dep>) cycle.get(i)).getValue().toVersionString());
            if (decorate) {
                buffer.append("^r^");
            }
        }
        return buffer.toString();
    }

    private static String getPathAsString(Vertex<Dep> vertex, DependencyAtom dependencyAtom) {
        if (vertex == null) {
            return null;
        }
        StringBuilder buffer = new StringBuilder();
        List<String> vertices = new ArrayList<String>();
        while (vertex != null) {
            vertices.add(vertex.getValue().toString());
            vertex = vertex.getAnyParent();
        }
        Collections.reverse(vertices);
        for (String vertexAsString : vertices) {
            if (buffer.length() > 0) {
                buffer.append(" -> ");
            }
            buffer.append(vertexAsString);
        }
        buffer.append(String.format(" -> ^b^%s:%s^r^", dependencyAtom.namespace, dependencyAtom.name));

        return buffer.toString();
    }

    /**
     * Resolves {@code dependencyAtom} by ensuring it's present in the local repository.  Resolution means iterating
     * over the repositories within {@code repositoryRegistry} and if {@code dependencyAtom} is found downloading or
     * copying it (if not already present) into the {@link RepositoryRegistry#localRepository} of {@code repositoryRegistry}
     * The {@code pomSufficient} exists as maven appears to be lax on transitive dependencies' transitive dependencies.
     * For instance, depending upon log4j:log4j:1.2.15 will fail b/c it depends upon javax.jms:jms:1.1 for which
     * there is no artifact in maven-central.  But if you depend upon org.apache.zookeeper:zookeeper:3.3.2
     * which depends upon log4j:log4j:1.2.15, resolution completes.  It will download the pom of
     * javax.jms:jms:1.1 but not require the packaged artifact (which is defaulted to jar) be present.  The
     * pom must be present however.
     * @param dependencyAtom to resolve
     * @param classifier to use when resolving transitive dependencies, or null
     * @param repositoryRegistry repositories to use when resolving {@code dependencyAtom}
     * @param pomSufficient if true, then only the pom from a maven repository is necessary to have successfully
     *                      resolved the {@code dependencyAtom}.
     * @param failMissingDependency true to print failure message on missing dependency
     * @return a {@link Dep} representation of {@code dependencyAtom} or null if {@code dependencyAtom} could
     *         not be resolved.
     */
    static Dep resolveDependency(DependencyAtom dependencyAtom, String classifier, RepositoryRegistry repositoryRegistry,
                                 boolean pomSufficient, boolean failMissingDependency) {
        // determine the local-repository directory for dependencyAtom; as it is needed regardless of where the dependency
        // if found.
        RepositoryAtom localRepo = repositoryRegistry.localRepository;

        // first consult the synthetic repository.
        if (repositoryRegistry.syntheticRepository != null && repositoryRegistry.syntheticRepository.containsKey(dependencyAtom)) {
            LocalPaths localPaths = LocalPaths.get(dependencyAtom, localRepo);
            return new Dep(dependencyAtom, repositoryRegistry.syntheticRepository.get(dependencyAtom), localPaths.localDirPath);
        }
        // not present within the synthetic, check the local and other (likely remote) repositories
        Dep resolved = resolveDependency(dependencyAtom, classifier, repositoryRegistry, localRepo, pomSufficient);
        if (resolved != null) {
            return resolved;
        }

        if (failMissingDependency) {
            Output.print("^error^ Dependency ^b^%s^r^ not found in any repository; ensure repositories are accessible.", dependencyAtom.toString());
            Output.print("^error^ Project's local repository is ^b^%s^r^.", localRepo.toString());
            int remoteRepoSize = repositoryRegistry.remoteRepositories.size();
            Output.print("^error^ Project has ^b^%d^r^ other repositor%s %s", remoteRepoSize, (remoteRepoSize != 1 ? "ies" : "y"),
                    (remoteRepoSize > 0 ? repositoryRegistry.remoteRepositories.toString() : ""));
        }
        return null;
    }

    private static Dep resolveDependency(DependencyAtom dependencyAtom, String classifier, RepositoryRegistry repositoryRegistry,
                                         RepositoryAtom localRepo, boolean pomSufficient) {
        LocalPaths localPaths = LocalPaths.get(dependencyAtom, localRepo);
        // also create pom-only objects
        DependencyAtom pomDependencyAtom = dependencyAtom.with("pom");
        LocalPaths localPomPaths = LocalPaths.get(pomDependencyAtom, localRepo);
        // check the local repository
        File localDepFile = new File(localPaths.localUrl.getFile());
        File localPomDepFile = new File(localPomPaths.localUrl.getFile());
        if (localDepFile.exists()) {
            return resolveDependency(dependencyAtom, classifier, localRepo, localPaths.localDirUrlPath, localPaths.localDirPath);
        } else if (pomSufficient && localPomDepFile.exists()) {
            return resolveDependency(pomDependencyAtom, classifier, localRepo, localPomPaths.localDirUrlPath, localPomPaths.localDirPath);
        }
        // not in the local repository, check each other repository.
        Dep resolved = resolveDependencyFromRemoteRepos(dependencyAtom, classifier, repositoryRegistry, localPaths, localDepFile);
        if ((resolved == null) && pomSufficient) {
            resolved = resolveDependencyFromRemoteRepos(pomDependencyAtom, classifier, repositoryRegistry, localPomPaths, localPomDepFile);
        }
        return resolved;
    }

    private static Dep resolveDependencyFromRemoteRepos(DependencyAtom dependencyAtom, String classifier,
                                                        RepositoryRegistry repositoryRegistry, LocalPaths localPaths,
                                                        File localDepFile) {
        List<RepositoryAtom> nonLocalRepos = repositoryRegistry.remoteRepositories;
        for (RepositoryAtom remoteRepo : nonLocalRepos) {
            String remotePathDir = getDependencyDirectoryPathForRepo(dependencyAtom, remoteRepo);
            String unauthRemotePath = FileUtil.pathFromParts(remotePathDir, dependencyAtom.getArtifactName());
            Auth auth = remoteRepo.getAuth();
            String remotePath;
            Map<String, String> headers = Collections.emptyMap();
            if (auth != null) {
                remotePath = auth.getArtifactPath(remotePathDir, dependencyAtom);
                headers = auth.getHeaders();
            } else {
                remotePath = unauthRemotePath;
            }
            URL remoteUrl = getUrl(remotePath);
            if (FileUtil.download(remoteUrl, headers, localDepFile, dependencyAtom.toString(), remoteRepo.toString(), true)) {
                return resolveDependency(dependencyAtom, classifier, remoteRepo, remotePathDir, localPaths.localDirPath);
            }
        }
        return null;
    }

    /**
     * Retrieves the direct dependencies of {@code dependencyAtom} (which is located within {@code repoDirPath}
     * of {@code repositoryAtom}) and saves them to {@code saveToRepoDirPath} as a dependencies property file.
     * @param dependencyAtom to retrieve the dependencies file
     * @param classifier to use when resolving transitive dependencies, or null
     * @param repositoryAtom from which {@code dependencyAtom} was resolved.
     * @param repoDirPath the directory location of {@code dependencyAtom} within the {@code repositoryAtom}.
     * @param saveToRepoDirPath to save the found dependency property file (should be within the local repository).
     * @return the property file associated with {@code dependencyAtom} (could be empty if {@code dependencyAtom}
     *         has no dependencies).
     */
    private static Dep resolveDependency(DependencyAtom dependencyAtom, String classifier, RepositoryAtom repositoryAtom,
                                         String repoDirPath, String saveToRepoDirPath) {
        PropFile dependenciesFile = getDependenciesFile(dependencyAtom, repositoryAtom, repoDirPath);
        if (dependenciesFile == null) {
            Output.print("^dbug^ No dependencies file found for %s in repo %s.", dependencyAtom.toString(), repositoryAtom.toString());
            dependenciesFile = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
        }
        // TODO - only store if necessary (also add force-update command like Maven's -U)
        storeDependenciesFile(dependenciesFile, saveToRepoDirPath);
        List<DependencyAtom> dependencyAtoms = parse(dependenciesFile, classifier);
        return new Dep(dependencyAtom, dependencyAtoms, saveToRepoDirPath);
    }

    /**
     * Converts {@code dependencies} into a list of {@link DependencyAtom} objects
     * @param dependencies to convert
     * @param classifier to use when constructing the {@link DependencyAtom} or null
     * @return the converted {@code dependencies}
     */
    public static List<DependencyAtom> parse(PropFile dependencies, String classifier) {
        List<DependencyAtom> dependencyAtoms = new ArrayList<DependencyAtom>();
        AtomicReference<String> error = new AtomicReference<String>();
        for (Prop dependency : dependencies.props()) {
            error.set(null);
            DependencyAtom dependencyAtom = parse(dependency);
            if (dependencyAtom == null) {
                continue;
            }
            if (classifier == null) {
                dependencyAtoms.add(dependencyAtom);
            } else {
                dependencyAtoms.add(dependencyAtom.withClassifier(classifier));
            }
        }
        return dependencyAtoms;
    }

    /**
     * Converts {@code dependencyProp} into a parsed {@link DependencyAtom}
     * @param dependencyProp to convert
     * @return the parsed {@link DependencyAtom} or null if {@code dependencyProp} could not be parsed
     */
    public static DependencyAtom parse(Prop dependencyProp) {
        if (dependencyProp == null) {
            return null;
        }
        AtomicReference<String> error = new AtomicReference<String>();
        String dependencyValue = dependencyProp.value();
        return parse(dependencyProp.name, dependencyValue, error);
    }

    private static DependencyAtom parse(String name, String value, AtomicReference<String> error) {
        DependencyAtom dependency = DependencyAtom.parse(name + ":" + value, error);
        if (dependency == null) {
            Output.print("^warn^ Invalid dependency %s:%s; %s", name, value, error.get());
            return null;
        }
        return dependency;
    }

    /**
     * Converts {@code exclusions} into a list of {@link DependencyAtom} objects
     *
     * @param exclusions to convert
     * @return the converted {@code exclusions}
     */
    public static List<DependencyAtom> parseExclusions(PropFile exclusions, String classifier) {
        List<DependencyAtom> exclusionAtoms = new ArrayList<DependencyAtom>();
        AtomicReference<String> error = new AtomicReference<String>();
        for (Prop exclusion : exclusions.props()) {
            if (exclusion.value().contains(" ")) {
                String[] versions = exclusion.value().split(" ");
                for (String version : versions) {
                    error.set(null);
                    parseExclusionVersion(exclusion, exclusionAtoms, version, classifier, error);
                }
            } else {
                error.set(null);
                parseExclusionVersion(exclusion, exclusionAtoms, exclusion.value(), classifier, error);
            }
        }
        return exclusionAtoms;
    }

    private static void parseExclusionVersion(Prop exclusion, List<DependencyAtom> exclusionAtoms, String version, String classifier,
                                              AtomicReference<String> error) {
        DependencyAtom exclusionAtom = parse(exclusion.name, version, error);
        if (exclusionAtom == null) {
            return;
        }
        if (classifier == null) {
            exclusionAtoms.add(exclusionAtom);
        } else {
            exclusionAtoms.add(exclusionAtom.withClassifier(classifier));
        }
    }

    /**
     * @param graph to convert into a resolved properties file
     * @return a {@link PropFile} object mapping each {@link Vertex<Dep>} (reachable from {@code graph}) object's
     *         {@link DependencyAtom} object's key (which is {@link Dep#dependencyAtom#getPropertyName()} + ":"
     *         + {@link Dep#dependencyAtom#getPropertyName()}) to the local repository location (which is
     *         {@link Dep#localRepositoryDirectory} + {@link File#separator}
     *         + {@link Dep#dependencyAtom#getArtifactName()}).
     *
     */
    public static PropFile convertToResolvedPropertiesFile(DirectedAcyclicGraph<Dep> graph) {
        final PropFile props = new PropFile(Context.named("resolved-deps"), PropFile.Loc.Local);
        Graphs.visit(graph, new Graphs.Visitor<Dep>() {
            @Override public void visit(Vertex<Dep> vertex) {
                Dep dep = vertex.getValue();
                // exclude transitive transient dependencies
                if (!vertex.isRoot() && dep.dependencyAtom.transientDep) {
                    return;
                }
                String dependencyAtomKey = dep.dependencyAtom.getPropertyName() + ":" + dep.dependencyAtom.getPropertyValue();
                String location = FileUtil.pathFromParts(dep.localRepositoryDirectory, dep.dependencyAtom.getArtifactName());
                if (!props.contains(dependencyAtomKey)) {
                    props.add(dependencyAtomKey, location);
                }
            }
        });
        return props;
    }

    /**
     * @param nullOnFNF if true then null is returned if the file is not found; otherwise, an empty {@link Properties}
     *                  is returned
     * @return the contents of ${project.build.dir}/${resolved-deps.properties} or an empty {@link Properties} if no
     *         such file is found and {@code nullOnFNF} is false otherwise null if no such file is found and
     *         {@code nullOnFNF} is true.
     */
    public static PropFile getResolvedProperties(boolean nullOnFNF) {
        Scope scope = Scope.named(Props.get("scope", Context.named("ply")).value());
        return getResolvedProperties(PlyUtil.LOCAL_CONFIG_DIR, scope, nullOnFNF);
    }

    /**
     * @param projectConfigDir the configuration directory from which to load properties like {@literal project.build.dir}
     * @param scope the scope of the resolved-deps property file.
     * @param nullOnFNF if true then null is returned if the file is not found; otherwise, an empty {@link Properties}
     *                  is returned
     * @return the contents of ${project.build.dir}/${resolved-deps.properties} relative to {@code projectConfigDir} or
     *         an empty {@link Properties} if no such file is found and {@code nullOnFNF} is false otherwise null if no
     *         such file is found and {@code nullOnFNF} is true.
     */
    public static PropFile getResolvedProperties(File projectConfigDir, Scope scope, boolean nullOnFNF) {
        String buildDir = Props.get("build.dir", Context.named("project"), Props.getScope(), projectConfigDir).value();
        File dependenciesFile = FileUtil.fromParts(FileUtil.getCanonicalPath(FileUtil.fromParts(projectConfigDir.getPath(), "..", "..")),
                                                   buildDir, "resolved-deps" + scope.getFileSuffix() + ".properties");
        PropFile resolvedDeps = new PropFile(Context.named("resolved-deps"), PropFile.Loc.Local);
        if (!dependenciesFile.exists()) {
            return (nullOnFNF ? null : resolvedDeps);
        }
        return PropFiles.load(dependenciesFile.getPath(), false, nullOnFNF);
    }

    /**
     * Constructs a classpath string for {@code resolvedDependencies} and {@code supplemental}
     * @param resolvedDependencies to add to the classpath
     * @param supplemental file references to add to the classpath
     * @return the classpath made up of {@code resolvedDependencies} and {@code supplemental}.
     */
    public static String getClasspath(PropFile resolvedDependencies, String ... supplemental) {
        StringBuilder classpath = new StringBuilder();
        for (Prop resolvedDependency : resolvedDependencies.props()) {
            if (DependencyAtom.isTransient(resolvedDependency.name)) {
                continue;
            }
            classpath.append(resolvedDependency.value());
            classpath.append(File.pathSeparator);
        }
        for (String sup : supplemental) {
            classpath.append(sup);
        }
        return classpath.toString();
    }

    /**
     * @return a {@link DependencyAtom} representing this project
     */
    public static DependencyAtom getProjectDep() {
        Context projectContext = Context.named("project");
        String namespace = Props.get("namespace", projectContext).value();
        String name = Props.get("name", projectContext).value();
        String version = Props.get("version", projectContext).value();
        String artifactName = Props.get("artifact.name", projectContext).value();
        return getDepFromParts(namespace, name, version, artifactName);
    }

    /**
     * @param namespace of the dependency
     * @param name of the dependency
     * @param version of the dependency
     * @param artifactName of the dependency
     * @return a {@link DependencyAtom} composed of parts {@code namespace}, {@code name}, {@code version} and {@code artifactName}
     */
    public static DependencyAtom getDepFromParts(String namespace, String name, String version, String artifactName) {
        String defaultArtifactName = getArtifactName(name, version, "", DependencyAtom.DEFAULT_PACKAGING);
        // don't pollute by placing artifactName explicitly even though it's the default
        if (artifactName.equals(defaultArtifactName)) {
            return new DependencyAtom(namespace, name, version);
        } else {
            return new DependencyAtom(namespace, name, version, artifactName);
        }
    }

    /**
     * The {@code name} and {@code version} must not be null.  If {@code classifier} is null/empty none is assumed.  If
     * {@code packaging} is null/empty then {@link DependencyAtom#DEFAULT_PACKAGING} is used.
     * @param name of the artifact
     * @param version of the artifact
     * @param classifier of the artifact (or null/empty)
     * @param packaging of the artifact (or null/empty)
     * @return the artifact name corresponding to {@code name}, {@code version}, {@code classifier} and {@code packaging}
     */
    public static String getArtifactName(String name, String version, String classifier, String packaging) {
        packaging = ((packaging == null) || packaging.isEmpty() ? DependencyAtom.DEFAULT_PACKAGING : packaging);
        if ((classifier == null) || classifier.isEmpty()) {
            return name + "-" + version + "." + packaging;
        } else {
            return name + "-" + version + "-" + classifier + packaging;
        }
    }

    /**
     * @param dependencyAtom for which to resolve the directory path within {@code repositoryAtom}
     * @param repositoryAtom from which to resolve the directory path for {@code dependencyAtom}
     * @return the directory path for {@code dependencyAtom} within {@code repositoryAtom}
     */
    public static String getDependencyDirectoryPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String startPath = Repos.getDirectoryPathForRepo(repositoryAtom);
        RepositoryAtom.Type type = repositoryAtom.getResolvedType();
        String namespace = (type == RepositoryAtom.Type.ply ? dependencyAtom.namespace : dependencyAtom.namespace.replaceAll("\\.", File.separator));
        String endPath = FileUtil.pathFromParts(namespace, dependencyAtom.name, dependencyAtom.version);
        return FileUtil.pathFromParts(startPath, endPath);
    }

    /**
     * @param dependencyAtom for which to resolve the artifact path within {@code repositoryAtom}
     * @param repositoryAtom from which to resolve the artifact path for {@code dependencyAtom}
     * @return the artifact path for {@code dependencyAtom} within {@code repositoryAtom}
     */
    public static String getDependencyArtifactPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String dependencyDirectoryPath = getDependencyDirectoryPathForRepo(dependencyAtom, repositoryAtom);
        return FileUtil.pathFromParts(dependencyDirectoryPath, dependencyAtom.getArtifactName());
    }

    /**
     * Adds {@code dependency} to the list of dependencies with changes which require audit during compilation.
     * As an example, if one updates the version of a dependency there could be breaking changes to all
     * the existing source code which depends upon the old dependency version. Marking this change here allows
     * the file-changed/compilation phase to process and recompile accordingly.
     * @param scope to use when saving the changed-deps file
     * @param dependency which changed
     */
    public static void addChangedDependency(Scope scope, DependencyAtom dependency) {
        PropFile.Prop localRepoProp = Props.get("localRepo", Context.named("depmngr"), scope, PlyUtil.LOCAL_CONFIG_DIR);
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp.value());
        PropFile changedDeps = loadChangedDependencies(scope);
        String location = FileUtil.pathFromParts(Deps.getDependencyDirectoryPathForRepo(dependency, localRepo),
                dependency.getArtifactName());
        // if the dependency was already changed, use existing as that's what the source code is tied to
        if (!changedDeps.contains(dependency.getPropertyName())) {
            changedDeps.add(dependency.getPropertyName(), location);
        }
        storeDependenciesChangedFile(changedDeps, scope);
    }

    private static PropFile loadChangedDependencies(Scope scope) {
        String storePath = getBuildDirStorePath("changed-deps", scope);
        PropFile propFile = new PropFile(Context.named("changed-deps"), PropFile.Loc.AdHoc);
        if (!PropFiles.load(storePath, propFile, true, false)) {
            Output.print("^error^Could not load ^b^%s^r^", storePath);
            System.exit(1);
        }
        return propFile;
    }

    private static void storeDependenciesChangedFile(PropFile changedDependencies, Scope scope) {
        String storePath = getBuildDirStorePath("changed-deps", scope);
        if (!PropFiles.store(changedDependencies, storePath, true)) {
            System.exit(1);
        }
    }

    private static String getBuildDirStorePath(String name, Scope scope) {
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        return buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator)
                + name + scope.getFileSuffix() + ".properties";
    }

    private static URL getUrl(String path) {
        try {
            path = ensureProtocol(path);
            return new URI(path.replaceAll("\\\\", "/")).toURL();
        } catch (URISyntaxException urise) {
            Output.print(urise);
            Output.print("^error^ for %s", path);
        } catch (MalformedURLException murle) {
            Output.print(murle);
            Output.print("^error^ for %s", path);
        } catch (IllegalArgumentException iae) {
            Output.print(iae);
            Output.print("^error^ for %s", path);
        }
        return null;
    }

    private static PropFile getDependenciesFile(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom,
                                                  String repoDepDir) {
        if (repositoryAtom.getResolvedType() == RepositoryAtom.Type.ply) {
            Auth auth = repositoryAtom.getAuth();
            String path;
            Map<String, String> headers = Collections.emptyMap();
            if (auth != null) {
                path = auth.getDependenciesPath(repoDepDir, "dependencies.properties");
                headers = auth.getHeaders();
            } else {
                path = FileUtil.pathFromParts(repoDepDir, "dependencies.properties");
            }
            return getDependenciesFromPlyRepo(path, headers);
        } else {
            // maven pom files are never saved with classifiers
            // @see 'classifier' under 'dependencies' in 'Pom Relationships' - http://maven.apache.org/pom.html
            DependencyAtom pom = dependencyAtom.withoutClassifier().with("pom");
            String pomName = pom.getArtifactName();
            return getDependenciesFromMavenRepo(FileUtil.pathFromParts(repoDepDir, pomName), repositoryAtom);
        }
    }

    private static PropFile getDependenciesFromPlyRepo(String urlPath, Map<String, String> headers) {
        URL url = getUrl(urlPath);
        if (url == null) {
            return null;
        }
        String localPath = FileUtil.getLocalPath(url, headers, urlPath, "[tmp location]");
        if ((localPath == null) || !new File(localPath).exists()) {
            return null;
        }
        PropFile loaded = PropFiles.load(localPath, false, false);
        if (loaded.isEmpty()) {
            return null;
        } else {
            return loaded;
        }
    }

    private static PropFile getDependenciesFromMavenRepo(String pomUrlPath, RepositoryAtom repositoryAtom) {
        MavenPomParser mavenPomParser = new MavenPomParser();
        MavenPom mavenPom = mavenPomParser.parsePom(pomUrlPath, repositoryAtom);
        return (mavenPom == null ? new PropFile(Context.named("dependencies"), PropFile.Loc.Local) : mavenPom.dependencies);
    }

    private static void storeDependenciesFile(PropFile transitiveDependencies, String localRepoDepDirPath) {
        PropFiles.store(transitiveDependencies, FileUtil.pathFromParts(localRepoDepDirPath, "dependencies.properties").replaceAll("\\\\", "/"), true);
    }

    private static String ensureProtocol(String localPath) {
        if (!localPath.contains(":")) {
            if (!localPath.startsWith("/")) {
                localPath = "/" + localPath;
            }
            // a file path without prefix, make absolute for URL handling
            localPath = "file://" + localPath;
        }
        return localPath;
    }

    private Deps() { }

}
