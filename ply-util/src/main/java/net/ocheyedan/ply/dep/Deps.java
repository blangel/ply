package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Graphs;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.mvn.MavenPom;
import net.ocheyedan.ply.mvn.MavenPomParser;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
            String localPath = getDependencyPathForRepo(dependencyAtom, localRepo);
            URL localUrl = getUrl(localPath);
            if (localUrl == null) {
                throw new AssertionError(String.format("The local path is not valid [ %s ]", localPath));
            }
            String localDirUrlPath = getDependencyDirectoryPathForRepo(dependencyAtom, localRepo);
            String localDirPath = (localDirUrlPath.startsWith("file://") ? localDirUrlPath.substring(7) : localDirUrlPath);
            return new LocalPaths(localPath, localUrl, localDirUrlPath, localDirPath);
        }

    }

    /**
     * @param dependencyAtoms the direct dependencies from which to create a dependency graph
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @return a DAG {@link Graph<Dep>} implementation representing the resolved {@code dependencyAtoms} and its tree of
     *         transitive dependencies.
     */
    public static DirectedAcyclicGraph<Dep> getDependencyGraph(List<DependencyAtom> dependencyAtoms,
                                                               RepositoryRegistry repositoryRegistry) {
        DirectedAcyclicGraph<Dep> dependencyDAG = new DirectedAcyclicGraph<Dep>();
        fillDependencyGraph(null, dependencyAtoms, repositoryRegistry, dependencyDAG, false);
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
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @param graph to fill with the resolved {@link Dep} objects of {@code dependencyAtoms}.
     * @param pomSufficient if true, then only the pom from a maven repository is necessary to have successfully
     *                      resolved the {@code dependencyAtom}.
     */
    private static void fillDependencyGraph(Vertex<Dep> parentVertex, List<DependencyAtom> dependencyAtoms,
                                            RepositoryRegistry repositoryRegistry, DirectedAcyclicGraph<Dep> graph,
                                            boolean pomSufficient) {
        if (repositoryRegistry.isEmpty()) {
            Output.print("^error^ No repositories found, cannot resolve dependencies.");
            System.exit(1);
        }
        for (DependencyAtom dependencyAtom : dependencyAtoms) {
            if ((parentVertex != null) && dependencyAtom.transientDep) {
                continue; // non-direct (transitive) transient dependencies should be skipped
            }
            // pom is sufficient for resolution if this is a transient dependency
            Dep resolvedDep = resolveDependency(dependencyAtom, repositoryRegistry, (pomSufficient || dependencyAtom.transientDep));
            if (resolvedDep == null) {
                String path = getPathAsString(parentVertex, dependencyAtom);
                if (path != null) {
                    Output.print("^error^ path to missing dependency [ %s ].", path);
                }
                System.exit(1);
            }
            Vertex<Dep> vertex = graph.addVertex(resolvedDep);
            if (parentVertex != null) {
                try {
                    graph.addEdge(parentVertex, vertex);
                } catch (Graph.CycleException gce) {
                    Output.print("^error^ circular dependency [ %s ].", getCycleAsString(gce));
                    System.exit(1);
                }
            }
            if (!dependencyAtom.transientDep) { // direct transient dependencies are not recurred upon
                fillDependencyGraph(vertex, vertex.getValue().dependencies, repositoryRegistry, graph, true);
            }
        }
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
     * @param repositoryRegistry repositories to use when resolving {@code dependencyAtom}
     * @param pomSufficient if true, then only the pom from a maven repository is necessary to have successfully
     *                      resolved the {@code dependencyAtom}.
     * @return a {@link Dep} representation of {@code dependencyAtom} or null if {@code dependencyAtom} could
     *         not be resolved.
     */
    public static Dep resolveDependency(DependencyAtom dependencyAtom, RepositoryRegistry repositoryRegistry,
                                        boolean pomSufficient) {
        // determine the local-repository directory for dependencyAtom; as it is needed regardless of where the dependency
        // if found.
        RepositoryAtom localRepo = repositoryRegistry.localRepository;

        // first consult the synthetic repository.
        if (repositoryRegistry.syntheticRepository != null && repositoryRegistry.syntheticRepository.containsKey(dependencyAtom)) {
            LocalPaths localPaths = LocalPaths.get(dependencyAtom, localRepo);
            return new Dep(dependencyAtom, repositoryRegistry.syntheticRepository.get(dependencyAtom), localPaths.localDirPath);
        }
        // not present within the synthetic, check the local and other (likely remote) repositories
        Dep resolved = resolveDependency(dependencyAtom, repositoryRegistry, localRepo, pomSufficient);
        if (resolved != null) {
            return resolved;
        }

        Output.print("^error^ Dependency ^b^%s^r^ not found in any repository; ensure repositories are accessible.", dependencyAtom.toString());
        Output.print("^error^ Project's local repository is ^b^%s^r^.", localRepo.toString());
        StringBuilder remoteRepoString = new StringBuilder();
        for (RepositoryAtom remote : repositoryRegistry.remoteRepositories) {
            if (remoteRepoString.length() != 0) {
                remoteRepoString.append(", ");
            }
            remoteRepoString.append(remote.toString());
        }
        int remoteRepoSize = repositoryRegistry.remoteRepositories.size();
        Output.print("^error^ Project has ^b^%d^r^ other repositor%s%s", remoteRepoSize, (remoteRepoSize != 1 ? "ies" : "y"),
                (remoteRepoSize > 0 ? String.format(" [ %s ]", remoteRepoString.toString()) : ""));
        return null;
    }

    private static Dep resolveDependency(DependencyAtom dependencyAtom, RepositoryRegistry repositoryRegistry,
                                         RepositoryAtom localRepo, boolean pomSufficient) {
        LocalPaths localPaths = LocalPaths.get(dependencyAtom, localRepo);
        // also create pom-only objects
        DependencyAtom pomDependencyAtom = dependencyAtom.with("pom");
        LocalPaths localPomPaths = LocalPaths.get(pomDependencyAtom, localRepo);
        // check the local repository
        File localDepFile = new File(localPaths.localUrl.getFile());
        File localPomDepFile = new File(localPomPaths.localUrl.getFile());
        if (localDepFile.exists()) {
            return resolveDependency(dependencyAtom, localRepo, localPaths.localDirUrlPath, localPaths.localDirPath);
        } else if (pomSufficient && localPomDepFile.exists()) {
            return resolveDependency(pomDependencyAtom, localRepo, localPomPaths.localDirUrlPath, localPomPaths.localDirPath);
        }
        // not in the local repository, check each other repository.
        Dep resolved = resolveDependencyFromRemoteRepos(dependencyAtom, repositoryRegistry, localPaths, localDepFile);
        if ((resolved == null) && pomSufficient) {
            resolved = resolveDependencyFromRemoteRepos(pomDependencyAtom, repositoryRegistry, localPomPaths, localPomDepFile);
        }
        return resolved;
    }

    private static Dep resolveDependencyFromRemoteRepos(DependencyAtom dependencyAtom,
                                                        RepositoryRegistry repositoryRegistry, LocalPaths localPaths,
                                                        File localDepFile) {
        List<RepositoryAtom> nonLocalRepos = repositoryRegistry.remoteRepositories;
        for (RepositoryAtom remoteRepo : nonLocalRepos) {
            String remotePathDir = getDependencyDirectoryPathForRepo(dependencyAtom, remoteRepo);
            String remotePath = FileUtil.pathFromParts(remotePathDir, dependencyAtom.getArtifactName());
            URL remoteUrl = getUrl(remotePath);
            if (remoteUrl == null) {
                continue;
            }
            InputStream stream;
            try {
                // TODO - proxy info (see http://download.oracle.com/javase/6/docs/technotes/guides/net/proxies.html)
                URLConnection urlConnection = remoteUrl.openConnection();
                stream = urlConnection.getInputStream();
            } catch (FileNotFoundException fnfe) {
                // this is fine, check next repo
                continue;
            } catch (IOException ioe) {
                Output.print(ioe); // TODO - parse exception and more gracefully handle http-errors.
                continue;
            }
            Output.print("^info^ Downloading %s from %s...", dependencyAtom.toString(), remoteRepo.toString());
            if (FileUtil.copy(stream, localDepFile)) {
                return resolveDependency(dependencyAtom, remoteRepo, remotePathDir, localPaths.localDirPath);
            }
        }
        return null;
    }

    /**
     * Retrieves the direct dependencies of {@code dependencyAtom} (which is located within {@code repoDirPath}
     * of {@code repositoryAtom}) and saves them to {@code saveToRepoDirPath} as a dependencies property file.
     * @param dependencyAtom to retrieve the dependencies file
     * @param repositoryAtom from which {@code dependencyAtom} was resolved.
     * @param repoDirPath the directory location of {@code dependencyAtom} within the {@code repositoryAtom}.
     * @param saveToRepoDirPath to save the found dependency property file (should be within the local repository).
     * @return the property file associated with {@code dependencyAtom} (could be empty if {@code dependencyAtom}
     *         has no dependencies).
     */
    private static Dep resolveDependency(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom,
                                         String repoDirPath, String saveToRepoDirPath) {
        Properties dependenciesFile = getDependenciesFile(dependencyAtom, repositoryAtom, repoDirPath);
        if (dependenciesFile == null) {
            Output.print("^dbug^ No dependencies file found for %s.", dependencyAtom.toString());
            dependenciesFile = new Properties();
        }
        storeDependenciesFile(dependenciesFile, saveToRepoDirPath);
        List<DependencyAtom> dependencyAtoms = parse(dependenciesFile);
        return new Dep(dependencyAtom, dependencyAtoms, saveToRepoDirPath);
    }

    /**
     * Converts {@code dependencies} into a list of {@link DependencyAtom} objects
     * @param dependencies to convert
     * @return the converted {@code dependencies}
     */
    public static List<DependencyAtom> parse(Map dependencies) {
        List<DependencyAtom> dependencyAtoms = new ArrayList<DependencyAtom>(dependencies.size());
        AtomicReference<String> error = new AtomicReference<String>();
        for (Object dependencyKey : dependencies.keySet()) {
            error.set(null);
            Object dependencyValue = dependencies.get(dependencyKey);
            DependencyAtom dependencyAtom = DependencyAtom.parse(dependencyKey + ":" + dependencyValue, error);
            if (dependencyAtom == null) {
                Output.print("^warn^ Invalid dependency %s:%s; missing %s", dependencyKey, dependencyValue,
                        error.get());
                continue;
            }
            dependencyAtoms.add(dependencyAtom);
        }
        return dependencyAtoms;
    }

    /**
     * @param graph to convert into a resolved properties file
     * @return a {@link Properties} object mapping each {@link Vertex<Dep>} (reachable from {@code graph}) object's
     *         {@link DependencyAtom} object's key (which is {@link Dep#dependencyAtom#getPropertyName()} + ":"
     *         + {@link Dep#dependencyAtom#getPropertyName()}) to the local repository location (which is
     *         {@link Dep#localRepositoryDirectory} + {@link File#separator}
     *         + {@link Dep#dependencyAtom#getArtifactName()}).
     *
     */
    public static Properties convertToResolvedPropertiesFile(DirectedAcyclicGraph<Dep> graph) {
        final Properties props = new Properties();
        Graphs.visit(graph, new Graphs.Visitor<Dep>() {
            @Override public void visit(Vertex<Dep> vertex) {
                Dep dep = vertex.getValue();
                // exclude transitive transient dependencies
                if (!vertex.isRoot() && dep.dependencyAtom.transientDep) {
                    return;
                }
                String dependencyAtomKey = dep.dependencyAtom.getPropertyName() + ":" + dep.dependencyAtom.getPropertyValue();
                String location = FileUtil.pathFromParts(dep.localRepositoryDirectory, dep.dependencyAtom.getArtifactName());
                if (!props.containsKey(dependencyAtomKey)) {
                    props.put(dependencyAtomKey, location);
                }
            }
        });
        return props;
    }

    /**
     * @return the contents of ${project.build.dir}/${resolved-deps.properties}
     */
    public static Properties getResolvedProperties() {
        String buildDir = Props.getValue("project", "build.dir");
        // load the resolved-deps.properties file from the build directory.
        String scope = Props.getValue("ply", "scope");
        String suffix = (scope.isEmpty() ? "" : scope + ".");
        File dependenciesFile = FileUtil.fromParts(buildDir, "resolved-deps." + suffix + "properties");
        if (!dependenciesFile.exists()) {
            return new Properties();
        }
        return PropertiesFileUtil.load(dependenciesFile.getPath());
    }

    /**
     * Constructs a classpath string for {@code resolvedDependencies} and {@code supplemental}
     * @param resolvedDependencies to add to the classpath
     * @param supplemental file references to add to the classpath
     * @return the classpath made up of {@code resolvedDependencies} and {@code supplemental}.
     */
    public static String getClasspath(Properties resolvedDependencies, String ... supplemental) {
        StringBuilder classpath = new StringBuilder();
        for (String resolvedDependency : resolvedDependencies.stringPropertyNames()) {
            if (DependencyAtom.isTransient(resolvedDependency)) {
                continue;
            }
            classpath.append(resolvedDependencies.getProperty(resolvedDependency));
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
        String namespace = Props.getValue("project", "namespace");
        String name = Props.getValue("project", "name");
        String version = Props.getValue("project", "version");
        String artifactName = Props.getValue("project", "artifact.name");
        String defaultArtifactName = name + "-" + version + "." + DependencyAtom.DEFAULT_PACKAGING;
        // don't pollute by placing artifactName explicitly even though it's the default
        if (artifactName.equals(defaultArtifactName)) {
            return new DependencyAtom(namespace, name, version);
        } else {
            return new DependencyAtom(namespace, name, version, artifactName);
        }
    }

    private static String getDependencyPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String dependencyDirectoryPath = getDependencyDirectoryPathForRepo(dependencyAtom, repositoryAtom);
        return FileUtil.pathFromParts(dependencyDirectoryPath, dependencyAtom.getArtifactName());
    }

    private static String getDependencyDirectoryPathForRepo(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom) {
        String startPath = repositoryAtom.getPropertyName();
        if (!startPath.contains(":")) {
            // a file path without prefix, make absolute
            startPath = "file://" + startPath;
        }
        RepositoryAtom.Type type = repositoryAtom.getResolvedType();
        String namespace = (type == RepositoryAtom.Type.ply ? dependencyAtom.namespace : dependencyAtom.namespace.replaceAll("\\.", File.separator));
        String endPath = FileUtil.pathFromParts(namespace, dependencyAtom.name, dependencyAtom.version);
        // hygiene the start separator
        if (endPath.startsWith("/") || endPath.startsWith("\\")) {
            endPath = endPath.substring(1, endPath.length());
        }
        return FileUtil.pathFromParts(startPath, endPath);
    }

    private static URL getUrl(String path) {
        try {
            return new URI(path).toURL();
        } catch (URISyntaxException urise) {
            Output.print(urise);
        } catch (MalformedURLException murle) {
            Output.print(murle);
        }
        return null;
    }

    private static Properties getDependenciesFile(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom,
                                                  String repoDepDir) {
        if (repositoryAtom.getResolvedType() == RepositoryAtom.Type.ply) {
            // no dependencies file just means no dependencies, skip.
            URL url = null;
            try {
                url = new URL(repoDepDir);
            } catch (MalformedURLException murle) {
                Output.print(murle);
            }
            if ((url == null) || !new File(url.getFile()).exists()) {
                return null;
            }
            return getDependenciesFromPlyRepo(FileUtil.pathFromParts(repoDepDir, "dependencies.properties"));
        } else {
            String pomName = dependencyAtom.getArtifactName().replace("." + dependencyAtom.getSyntheticPackaging(), ".pom");
            return getDependenciesFromMavenRepo(FileUtil.pathFromParts(repoDepDir, pomName), repositoryAtom);
        }
    }

    private static Properties getDependenciesFromPlyRepo(String urlPath) {
        try {
            URL url = new URL(urlPath);
            return PropertiesFileUtil.load(url.getFile(), false, true);
        } catch (MalformedURLException murle) {
            Output.print(murle);
        }
        return null;
    }

    private static Properties getDependenciesFromMavenRepo(String pomUrlPath, RepositoryAtom repositoryAtom) {
        MavenPomParser mavenPomParser = new MavenPomParser();
        MavenPom mavenPom = mavenPomParser.parsePom(pomUrlPath, repositoryAtom);
        return (mavenPom == null ? new Properties() : mavenPom.dependencies);
    }

    private static void storeDependenciesFile(Properties transitiveDependencies, String localRepoDepDirPath) {
        PropertiesFileUtil.store(transitiveDependencies, FileUtil.pathFromParts(localRepoDepDirPath, "dependencies.properties"), true);
    }

    private Deps() { }

}
