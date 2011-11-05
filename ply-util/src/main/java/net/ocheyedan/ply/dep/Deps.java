package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Graphs;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.mvn.MavenPomParser;
import net.ocheyedan.ply.props.Props;

import java.io.*;
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
     * @param dependencyAtoms the direct dependencies from which to create a dependency graph
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @return a DAG {@link Graph<Dep>} implementation representing the resolved {@code dependencyAtoms} and its tree of
     *         transitive dependencies.
     */
    public static DirectedAcyclicGraph<Dep> getDependencyGraph(List<DependencyAtom> dependencyAtoms,
                                                               RepositoryRegistry repositoryRegistry) {
        DirectedAcyclicGraph<Dep> dependencyDAG = new DirectedAcyclicGraph<Dep>();
        fillDependencyGraph(null, dependencyAtoms, repositoryRegistry, dependencyDAG);
        return dependencyDAG;
    }

    /**
     * For each resolved {@link Dep} object of {@code dependencyAtoms} a {@link Vertex<Dep>} will be created and added
     * to {@code graph}.  If {@code parentVertex} is not null, an edge will be added from {@code parentVertex} and
     * each resolved property.  If adding such an edge violates the acyclic nature of {@code graph} an error message
     * will be printed and this program will halt.
     * @param parentVertex the parent of {@code dependencyAtoms}
     * @param dependencyAtoms the dependencies which to resolve and place into {@code graph}
     * @param repositoryRegistry the repositories to consult when resolving {@code dependencyAtoms}.
     * @param graph to fill with the resolved {@link Dep} objects of {@code dependencyAtoms}.
     */
    private static void fillDependencyGraph(Vertex<Dep> parentVertex, List<DependencyAtom> dependencyAtoms,
                                            RepositoryRegistry repositoryRegistry, DirectedAcyclicGraph<Dep> graph) {
        if (repositoryRegistry.isEmpty()) {
            throw new IllegalArgumentException("Need at least one repository!");
        }
        for (DependencyAtom dependencyAtom : dependencyAtoms) {
            Dep resolvedDep = resolveDependency(dependencyAtom, repositoryRegistry);
            if (resolvedDep == null) {
                System.exit(1);
            }
            Vertex<Dep> vertex = graph.addVertex(resolvedDep);
            if (parentVertex != null) {
                try {
                    graph.addEdge(parentVertex, vertex);
                } catch (Graph.CycleException gce) {
                    Output.print("^error^ circular dependency [ %s ].", gce.cycleToString());
                    System.exit(1);
                }
            }
            fillDependencyGraph(vertex, vertex.getValue().dependencies, repositoryRegistry, graph);
        }
    }

    /**
     * Resolves {@code dependencyAtom} by ensuring it's present in the local repository.  Resolution means iterating
     * over {@code repositoryAtoms} and if {@code dependencyAtom} is found downloading or copying it into the first
     * item within {@code repositoryAtoms} which must be the local repository (downloading/copying only if it is not
     * already present in the first item in {@code repositoryAtoms}).
     * @param dependencyAtom to resolve
     * @param repositoryRegistry repositories to use when resolving {@code dependencyAtom}
     * @return a {@link Dep} representation of {@code dependencyAtom} or null if {@code dependencyAtom} could
     *         not be resolved.
     */
    private static Dep resolveDependency(DependencyAtom dependencyAtom, RepositoryRegistry repositoryRegistry) {
        // determine the local-repository directory for dependencyAtom; as it is needed regardless of where the dependency
        // if found.
        RepositoryAtom localRepo = repositoryRegistry.localRepository;
        String localPath = getDependencyPathForRepo(dependencyAtom, localRepo);
        URL localUrl = getUrl(localPath);
        if (localUrl == null) {
            throw new AssertionError(String.format("The local path is not valid [ %s ]", localPath));
        }
        String localDirUrlPath = getDependencyDirectoryPathForRepo(dependencyAtom, localRepo);
        String localDirPath = (localDirUrlPath.startsWith("file://") ? localDirUrlPath.substring(7) : localDirUrlPath);

        // first consult the synthetic repository.
        if (repositoryRegistry.syntheticRepository != null && repositoryRegistry.syntheticRepository.containsKey(dependencyAtom)) {
            return new Dep(dependencyAtom, repositoryRegistry.syntheticRepository.get(dependencyAtom), localDirPath);
        }

        // not present within the synthetic, check the local
        File localDepFile = new File(localUrl.getFile());
        if (localDepFile.exists()) {
            return resolveDependency(dependencyAtom, localRepo, localDirUrlPath, localDirPath);
        }

        // not in the local repository, check each other repository.
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
                Output.print(ioe);
                continue;
            }
            Output.print("^info^ Downloading %s from %s...", dependencyAtom.toString(), remoteRepo.toString());
            if (FileUtil.copy(stream, localDepFile)) {
                return resolveDependency(dependencyAtom, remoteRepo, remotePathDir, localDirPath);
            }
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
        Output.print("^error^ Project has ^b^%d^r^ remote repositor%s%s", remoteRepoSize, (remoteRepoSize != 1 ? "ies" : "y"),
                     (remoteRepoSize > 0 ? String.format(" [ %s ]", remoteRepoString.toString()) : ""));
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
        MavenPomParser mavenPomParser = new MavenPomParser.Default();
        return mavenPomParser.parsePom(pomUrlPath, repositoryAtom);
    }

    private static void storeDependenciesFile(Properties transitiveDependencies, String localRepoDepDirPath) {
        PropertiesFileUtil.store(transitiveDependencies, FileUtil.pathFromParts(localRepoDepDirPath, "dependencies.properties"), true);
    }

    private Deps() { }

}
