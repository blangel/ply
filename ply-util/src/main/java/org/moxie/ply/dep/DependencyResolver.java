package org.moxie.ply.dep;

import org.moxie.ply.FileUtil;
import org.moxie.ply.Output;
import org.moxie.ply.PropertiesUtil;
import org.moxie.ply.mvn.MavenPomParser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 1:41 PM
 */
public class DependencyResolver {

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
        if (localDepFile.exists()) {
            // use the unresolved property value as we don't want to pollute by specifying an artifactName when it
            // is in fact just the default.
            String dependencyAtomKey = dependencyAtom.getPropertyName() + "::" + dependencyAtom.getPropertyValue();
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
            String remotePath = remotePathDir + File.separator + dependencyAtom.getArtifactName();
            URL remoteUrl = getUrl(remotePath);
            if (remoteUrl == null) {
                continue;
            }
            if (FileUtil.copy(remoteUrl, localDepFile)) {
                if (!dependencyFiles.contains(localDepFile.getPath())) {
                    dependencyFiles.put(localDepFile.getPath(), "");
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
            Output.print("^dbug^ resolving %s", path);
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
            DependencyAtom transitiveDependencyAtom = DependencyAtom.parse(dependency + "::" + dependencyVersion, error);
            if (transitiveDependencyAtom == null) {
                Output.print("^info^ Dependency %s::%s invalid; missing %s, skipping.", dependency,
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
            return getTransitiveDependenciesFromPlyRepo(repoDepDir + File.separator + "dependencies.properties");
        } else {
            String pomName = dependencyAtom.getArtifactName().replace(".jar", ".pom");
            return getTransitiveDependenciesFromMavenRepo(repoDepDir + File.separator + pomName, repositoryAtom);
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
        PropertiesUtil.store(transitiveDependencies, localRepoDepDirPath + (localRepoDepDirPath.endsWith(File.separator) ? "" : File.separator)
                                                + "dependencies.properties", true);
    }

}
