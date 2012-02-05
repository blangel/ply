package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.OrderedProperties;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.util.Properties;

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
        String buildDirPath = Props.getValue(Context.named("project"), "build.dir");
        String artifactName = Props.getValue(Context.named("project"), "artifact.name");
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return;
        }

        String plyProjectDirPath = Props.getValue(Context.named("ply"), "project.dir");
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", "dependencies.properties");

        String localRepoProp = Props.getValue(Context.named("depmngr"), "localRepo");
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp);
        String namespace = Props.getValue(Context.named("project"), "namespace");
        String name = Props.getValue(Context.named("project"), "name");
        String version = Props.getValue(Context.named("project"), "version");
        if (localRepo == null) {
            Output.print("^error^ Local repository ^b^%s^r^ doesn't exist.", localRepoProp);
            System.exit(1);
        }
        String convertedNamespace = (localRepo.isPlyType() ? namespace : namespace.replaceAll("\\.", File.separator));
        String localRepoPath = Deps.getDirectoryPathForRepo(localRepo);
        String localRepoArtifactBasePath = FileUtil.pathFromParts(localRepoPath, convertedNamespace, name, version);
        File localRepoArtifact = FileUtil.fromParts(localRepoArtifactBasePath, artifactName);
        FileUtil.copy(artifact, localRepoArtifact);

        File localRepoDependenciesFile = FileUtil.fromParts(localRepoArtifactBasePath, "dependencies.properties");
        if (dependenciesFile.exists()) {
            FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        } else {
            // need to override (perhaps there were dependencies but now none.
            Properties dependencies = new OrderedProperties();
            PropertiesFileUtil.store(dependencies, localRepoDependenciesFile.getPath(), true);
        }
    }

}