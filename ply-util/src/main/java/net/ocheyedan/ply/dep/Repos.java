package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFiles;
import net.ocheyedan.ply.props.Props;

import java.io.File;

/**
 * User: blangel
 * Date: 1/4/13
 * Time: 11:31 AM
 *
 * Utility class to interact with {@literal repository} urls.
 */
public final class Repos {

    /**
     * Copies the project artifact ({@literal project.artifact.name} from {@literal project.build.dir}) into
     * {@code localRepo}
     * @param localRepo into which to install the artifact
     * @return true on success, false if the artifact does not exist or there was an error saving
     */
    public static boolean install(RepositoryAtom localRepo) {

        String buildDirPath = Props.get("build.dir", Context.named("project")).value();
        String artifactName = Props.get("artifact.name", Context.named("project")).value();
        File artifact = FileUtil.fromParts(buildDirPath, artifactName);
        if (!artifact.exists()) {
            return false;
        }

        String plyProjectDirPath = Props.get("project.dir", Context.named("ply")).value();
        File dependenciesFile = FileUtil.fromParts(plyProjectDirPath, "config", "dependencies.properties");

        String namespace = Props.get("namespace", Context.named("project")).value();
        String name = Props.get("name", Context.named("project")).value();
        String version = Props.get("version", Context.named("project")).value();
        String convertedNamespace = (localRepo.isPlyType() ? namespace : namespace.replaceAll("\\.", File.separator));
        String localRepoPath = Deps.getDirectoryPathForRepo(localRepo);
        String localRepoArtifactBasePath = FileUtil.pathFromParts(localRepoPath, convertedNamespace, name, version);
        File localRepoArtifact = FileUtil.fromParts(localRepoArtifactBasePath, artifactName);
        FileUtil.copy(artifact, localRepoArtifact);

        File localRepoDependenciesFile = FileUtil.fromParts(localRepoArtifactBasePath, "dependencies.properties");
        if (dependenciesFile.exists()) {
            return FileUtil.copy(dependenciesFile, localRepoDependenciesFile);
        } else {
            // need to override (perhaps there were dependencies but now none.
            PropFile dependencies = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
            return PropFiles.store(dependencies, localRepoDependenciesFile.getPath(), true);
        }
    }

    private Repos() { }

}
