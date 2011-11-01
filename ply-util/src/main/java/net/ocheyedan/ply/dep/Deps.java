package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.mvn.MavenPomParser;
import net.ocheyedan.ply.props.Props;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:41 PM
 */
public class Deps {

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

    public static Properties resolveDependencies(List<DependencyAtom> dependencyAtoms, List<RepositoryAtom> repositoryAtoms) {
        Properties dependencyFiles = new Properties();

        for (DependencyAtom dependencyAtom : dependencyAtoms) {
            resolveDependency(dependencyAtom, repositoryAtoms, dependencyFiles);
        }

        return dependencyFiles;
    }

    public static boolean resolveDependency(DependencyAtom dependencyAtom, List<RepositoryAtom> repositories,
                                             Properties dependencyFiles) {
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
        String dependencyAtomKey = dependencyAtom.getPropertyName() + ":" + dependencyAtom.getPropertyValue();
        if (localDepFile.exists()) {
            // use the unresolved property value as we don't want to pollute by specifying an artifactName when it
            // is in fact just the default.
            if (!dependencyFiles.contains(dependencyAtomKey)) {
                dependencyFiles.put(dependencyAtomKey, localDepFile.getPath());
                // TODO - should this also resave the transitive deps file?
                processTransitiveDependencies(dependencyAtom, localRepo, repositories,
                                                "file://" + localDepDirFile.getPath(), dependencyFiles);

            }
            return true;
        }

        // not in the local-repo, check each other repo.
        for (RepositoryAtom remoteRepo : nonLocalRepos) {
            String remotePathDir = getDependencyDirectoryPathForRepo(dependencyAtom, remoteRepo);
            String remotePath = FileUtil.pathFromParts(remotePathDir, dependencyAtom.getArtifactName());
            URL remoteUrl = getUrl(remotePath);
            if (remoteUrl == null) {
                continue;
            }
            InputStream stream;
            try {
                URLConnection urlConnection = remoteUrl.openConnection();
                stream = urlConnection.getInputStream();
            } catch(FileNotFoundException fnfe) {
                // this is fine, check next repo
                continue;
            } catch (IOException ioe) {
                Output.print(ioe);
                continue;
            }
            if (FileUtil.copy(stream, localDepFile)) {
                if (!dependencyFiles.contains(dependencyAtomKey)) {
                    dependencyFiles.put(dependencyAtomKey, localDepFile.getPath());
                    Properties transitiveDeps = processTransitiveDependencies(dependencyAtom, remoteRepo, repositories,
                                                    remotePathDir, dependencyFiles);
                    storeTransitiveDependenciesFile(transitiveDeps, localDepDirFile.getPath());
                }
                return true;
            }
        }
        Output.print("^warn^ Dependency ^b^%s^r^ not found in any repository.", dependencyAtom.toString());
        return false;
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

    private static Properties processTransitiveDependencies(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom, List<RepositoryAtom> repositories,
                                                      String repoDepDir, Properties resolvedDependencies) {
        Properties transitiveDependencies = getTransitiveDependenciesFile(dependencyAtom, repositoryAtom, repoDepDir);
        if (transitiveDependencies == null) {
            Output.print("^dbug^ No dependencies file found for %s, ignoring.",
                    dependencyAtom.toString());
            return null;
        }
        AtomicReference<String> error = new AtomicReference<String>();
        for (String dependency : transitiveDependencies.stringPropertyNames()) {
            error.set(null);
            String dependencyVersion = transitiveDependencies.getProperty(dependency);
            DependencyAtom transitiveDependencyAtom = DependencyAtom.parse(dependency + ":" + dependencyVersion, error);
            if (transitiveDependencyAtom == null) {
                Output.print("^warn^ Dependency %s:%s invalid; missing %s, skipping.", dependency,
                        dependencyVersion, error.get());
                continue;
            }
            resolveDependency(transitiveDependencyAtom, repositories, resolvedDependencies);
        }
        return transitiveDependencies;
    }

    private static Properties getTransitiveDependenciesFile(DependencyAtom dependencyAtom, RepositoryAtom repositoryAtom,
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
            return getTransitiveDependenciesFromPlyRepo(FileUtil.pathFromParts(repoDepDir, "dependencies.properties"));
        } else {
            String pomName = dependencyAtom.getArtifactName().replace("." + dependencyAtom.getSyntheticPackaging(), ".pom");
            return getTransitiveDependenciesFromMavenRepo(FileUtil.pathFromParts(repoDepDir, pomName), repositoryAtom);
        }
    }

    private static Properties getTransitiveDependenciesFromPlyRepo(String urlPath) {
        InputStream inputStream = null;
        try {
            URL url = new URL(urlPath);
            Properties properties = new Properties();
            inputStream = new BufferedInputStream(url.openStream());
            properties.load(inputStream);
            return properties;
        } catch (MalformedURLException murle) {
            Output.print(murle);
        } catch (FileNotFoundException fnfe) {
            return null; // not a problem, just means there are no dependencies
        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException ioe) {
                // ignore
            }
        }
        return null;
    }

    private static Properties getTransitiveDependenciesFromMavenRepo(String pomUrlPath, RepositoryAtom repositoryAtom) {
        MavenPomParser mavenPomParser = new MavenPomParser.Default();
        return mavenPomParser.parsePom(pomUrlPath, repositoryAtom);
    }

    private static void storeTransitiveDependenciesFile(Properties transitiveDependencies, String localRepoDepDirPath) {
        PropertiesFileUtil.store(transitiveDependencies, FileUtil.pathFromParts(localRepoDepDirPath, "dependencies.properties"), true);
    }

}
