package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Props;

import java.io.File;

/**
 * User: blangel
 * Date: 1/14/13
 * Time: 12:50 PM
 *
 * Copies the resolved dependencies (the {@literal resolved-deps} file within the {@literal project.build.dir})
 * into {@literal depcopy.dir}, deleting all files within {@literal depcopy.dir} first.
 */
public final class DependencyCopy {

    public static void main(String[] args) {
        PropFile resolvedProperties = Deps.getResolvedProperties(false);
        String toDirPath = Props.get("dir", Context.named("depcopy")).value();
        if (toDirPath.isEmpty()) {
            Output.print("^warn^ The ^b^depcopy.dir^r^ property is null, skipping dependency copying.");
            return;
        }
        File copyToDir = new File(toDirPath);
        FileUtil.delete(copyToDir);
        copyDependencies(resolvedProperties, copyToDir);
    }

    protected static void copyDependencies(PropFile resolvedProperties, File copyToDir) {
        copyToDir.mkdirs();
        for (PropFile.Prop resolvedKey : resolvedProperties.props()) {
            if (DependencyAtom.isTransient(resolvedKey.name)) {
                continue;
            }
            File dependency = new File(resolvedKey.value());
            File to = FileUtil.fromParts(copyToDir.getPath(), dependency.getName());
            FileUtil.copy(dependency, to);
        }
    }

}
