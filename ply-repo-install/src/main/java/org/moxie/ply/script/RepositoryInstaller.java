package org.moxie.ply.script;

import java.io.*;

/**
 * User: blangel
 * Date: 10/1/11
 * Time: 12:52 PM
 *
 * Copies the project artifact and its dependencies property file to the local repository.
 * This script has no properties inherit to it but leverages the following properties:
 * {@literal project.build.dir}/{@literal project.artifact.name} - location and name of the artifact to copy.
 * {@literal depmngr.localRepo}/{@literal project.namespace}/{@literal project.name}/{@literal project.version} - location
 * into which to copy the project artifact and its dependencies property file.
 * {@literal ply.project.dir}/config/dependencies.properties - the dependencies file to copy
 *
 * If the artifact doesn't exist at the above location, this script silently exists.
 */
public class RepositoryInstaller {

    public static void main(String[] args) {
        String buildDirPath = System.getenv("project.build.dir");
        String artifactName = System.getenv("project.artifact.name");
        File artifact = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator) +
                                    artifactName);
        if (!artifact.exists()) {
            return;
        }

        String plyProjectDirPath = System.getenv("ply.project.dir");
        File dependenciesFile = new File(plyProjectDirPath + (plyProjectDirPath.endsWith(File.separator) ? "" : File.separator)
                                            + "config/dependencies.properties");

        String localRepoProp = System.getenv("depmngr.localRepo");
        // determine repo type.
        boolean localRepoIsPly = true;
        String[] resolvedLocalRepo = localRepoProp.split("::");
        if (resolvedLocalRepo.length == 2) {
            String type = resolvedLocalRepo[1];
            if ("maven".equals(type)) {
                localRepoIsPly = false;
            }
        }

        String namespace = System.getenv("project.namespace");
        String name = System.getenv("project.name");
        String version = System.getenv("project.version");
        File localRepoBase = new File(resolvedLocalRepo[0]);
        if (!localRepoBase.exists()) {
            System.out.printf("^error^ Local repository ^b^%s^r^ doesn't exist.\n", localRepoBase.getPath());
            System.exit(1);
        }
        String localRepoPath = localRepoBase.getPath() + (localRepoBase.getPath().endsWith(File.separator) ? "" : File.separator) +
                               (localRepoIsPly ? namespace : namespace.replaceAll("\\.", File.separator)) +
                                    (namespace.endsWith(File.separator) ? "" : File.separator) +
                               name + (name.endsWith(File.separator) ? "" : File.separator) +
                               version + (version.endsWith(File.separator) ? "" : File.separator);
        File localRepo = new File(localRepoPath);
        localRepo.mkdirs();
        File localRepoArtifact = new File(localRepoPath + artifactName);
        copy(artifact, localRepoArtifact);

        if (dependenciesFile.exists()) {
            File localRepoDependenciesFile = new File(localRepoPath + "dependencies.properties");
            copy(dependenciesFile, localRepoDependenciesFile);
        }
    }

    private static void copy(File from, File to) {
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            to.createNewFile();
            inputStream = new BufferedInputStream(new FileInputStream(from));
            outputStream = new BufferedOutputStream(new FileOutputStream(to));
            byte[] tx = new byte[8192];
            int read;
            while ((read = inputStream.read(tx)) != -1) {
                outputStream.write(tx, 0, read);
            }
        } catch (IOException ioe) {
            System.out.printf("^error^ %s\n", ioe);
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
    }

}
