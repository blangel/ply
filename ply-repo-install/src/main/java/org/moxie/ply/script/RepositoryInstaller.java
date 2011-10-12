package org.moxie.ply.script;

import org.moxie.ply.FileUtil;
import org.moxie.ply.Output;
import org.moxie.ply.dep.Deps;
import org.moxie.ply.dep.RepositoryAtom;
import org.moxie.ply.props.Props;

import java.io.File;

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
        String buildDirPath = Props.getValue("project", Props.DEFAULT_SCOPE, "build.dir");
        String artifactName = Props.getValue("project", Props.DEFAULT_SCOPE, "artifact.name");
        File artifact = new File(buildDirPath + (buildDirPath.endsWith(File.separator) ? "" : File.separator) +
                                    artifactName);
        if (!artifact.exists()) {
            return;
        }

        String plyProjectDirPath = Props.getValue("project.dir");
        File dependenciesFile = new File(plyProjectDirPath + (plyProjectDirPath.endsWith(File.separator) ? "" : File.separator)
                                            + "config/dependencies.properties");

        String localRepoProp = Props.getValue("depmngr", Props.DEFAULT_SCOPE, "localRepo");
        // determine repo type.
        boolean localRepoIsPly = !(localRepoProp.startsWith(RepositoryAtom.MAVEN_REPO_TYPE_PREFIX));
        if (!localRepoIsPly) {
            localRepoProp = localRepoProp.substring(RepositoryAtom.MAVEN_REPO_TYPE_PREFIX.length());
        } else if (localRepoProp.startsWith(RepositoryAtom.PLY_REPO_TYPE_PREFIX)) {
            localRepoProp = localRepoProp.substring(RepositoryAtom.PLY_REPO_TYPE_PREFIX.length());
        }

        String namespace = Props.getValue("project", Props.DEFAULT_SCOPE, "namespace");
        String name = Props.getValue("project", Props.DEFAULT_SCOPE, "name");
        String version = Props.getValue("project", Props.DEFAULT_SCOPE, "version");
        File localRepoBase = new File(localRepoProp);
        if (!localRepoBase.exists()) {
            Output.print("^error^ Local repository ^b^%s^r^ doesn't exist.", localRepoBase.getPath());
            System.exit(1);
        }
        String localRepoPath = localRepoBase.getPath() + (localRepoBase.getPath().endsWith(File.separator) ? "" : File.separator) +
                               (localRepoIsPly ? namespace : namespace.replaceAll("\\.", File.separator)) +
                                    (namespace.endsWith(File.separator) ? "" : File.separator) +
                               name + (name.endsWith(File.separator) ? "" : File.separator) +
                               version + (version.endsWith(File.separator) ? "" : File.separator);
        File localRepoArtifact = new File(localRepoPath + artifactName);
        FileUtil.copy(artifact, localRepoArtifact);

        if (dependenciesFile.exists()) {
            File localRepoDependenciesFile = new File(localRepoPath + "dependencies.properties");
            FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        }
    }

}