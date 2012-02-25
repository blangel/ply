package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFiles;
import net.ocheyedan.ply.props.Props;

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
        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        String artifactName = Props.get("artifact.name", Context.named("project")).value();
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return;
        }

        String plyProjectDirPath = Props.get("project.dir", Context.named("ply")).value();
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", "dependencies.properties");

        String localRepoProp = Props.get("localRepo", Context.named("depmngr")).value();
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp);
        String namespace = Props.get("namespace", Context.named("project")).value();
        String name = Props.get("name", Context.named("project")).value();
        String version = Props.get("version", Context.named("project")).value();
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
            PropFile dependencies = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
            PropFiles.store(dependencies, localRepoDependenciesFile.getPath(), true);
        }
    }

}