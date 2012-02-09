package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * User: blangel
 * Date: 10/14/11
 * Time: 1:59 PM
 *
 * Simply copies {@literal project[.scope].res.dir} to {@literal project[.scope].res.build.dir} priming the resources
 * for filtering/packaging/etc.
 */
public final class ResourcesScript {

    public static void main(String[] args) {
        Prop resourcesDirProp = Props.get(Context.named("project"), "res.dir");
        Prop resourcesBuildDirProp = Props.get(Context.named("project"), "res.build.dir");
        String resourcesExclusionPropValue = Props.getValue(Context.named("resources"), "exclude");
        if ((resourcesDirProp == null) || (resourcesBuildDirProp == null)) {
            Output.print("^error^ Could not find properties 'project.res.dir' or 'project.res.build.dir'");
            System.exit(1);
        }
        String[] split = resourcesExclusionPropValue.split(",");
        final Set<String> exclusions = new HashSet<String>(Arrays.asList(split));

        File resDir = new File(resourcesDirProp.value);
        if (!resDir.exists()) {
            // nothing to copy.
            Output.print("^dbug^ No resources to copy.");
            return;
        }

        File resBuildDir = new File(resourcesBuildDirProp.value);
        FilenameFilter excluding = (exclusions.isEmpty() ? null : new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return exclusions.contains(name);
            }
        });
        if (!FileUtil.copyDir(resDir, resBuildDir, excluding)) {
            Output.print("^error^ Could not copy resources directory (%s) to resources build directory (%s).",
                    resourcesDirProp.value, resourcesBuildDirProp.value);
            System.exit(1);
        }
    }

    private ResourcesScript() { }

}
