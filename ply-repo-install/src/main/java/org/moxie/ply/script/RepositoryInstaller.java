package org.moxie.ply.script;

import org.moxie.ply.FileUtil;
import org.moxie.ply.Output;
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
        String buildDirPath = Props.getValue("project", "build.dir");
        String artifactName = Props.getValue("project", "artifact.name");
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return;
        }

        String plyProjectDirPath = Props.getValue("project.dir");
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", "dependencies.properties");

        String localRepoProp = Props.getValue("depmngr", "localRepo");
        // determine repo type.
        boolean localRepoIsPly = !(localRepoProp.startsWith(RepositoryAtom.MAVEN_REPO_TYPE_PREFIX));
        if (!localRepoIsPly) {
            localRepoProp = localRepoProp.substring(RepositoryAtom.MAVEN_REPO_TYPE_PREFIX.length());
        } else if (localRepoProp.startsWith(RepositoryAtom.PLY_REPO_TYPE_PREFIX)) {
            localRepoProp = localRepoProp.substring(RepositoryAtom.PLY_REPO_TYPE_PREFIX.length());
        }

        String namespace = Props.getValue("project", "namespace");
        String name = Props.getValue("project", "name");
        String version = Props.getValue("project", "version");
        File localRepoBase = new File(localRepoProp);
        if (!localRepoBase.exists()) {
            Output.print("^error^ Local repository ^b^%s^r^ doesn't exist.", localRepoBase.getPath());
            System.exit(1);
        }
        String convertedNamespace = (localRepoIsPly ? namespace : namespace.replaceAll("\\.", File.separator));
        String localRepoPath = FileUtil.pathFromParts(localRepoBase.getPath(), convertedNamespace, name, version);
        File localRepoArtifact = FileUtil.fromParts(localRepoPath, artifactName);
        FileUtil.copy(artifact, localRepoArtifact);

        if (dependenciesFile.exists()) {
            File localRepoDependenciesFile = FileUtil.fromParts(localRepoPath, "dependencies.properties");
            FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        }
    }

}