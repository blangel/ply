package org.moxie.ply.script;

import org.moxie.ply.FileUtil;
import org.moxie.ply.Output;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import java.io.File;

/**
 * User: blangel
 * Date: 10/14/11
 * Time: 1:59 PM
 *
 * Simply copies {@literal project[.scope].res.dir} to {@literal project[.scope].res.build.dir} priming the resources
 * for filtering/packaging/etc.
 */
public class ResourcesScript {

    public static void main(String[] args) {
        Prop resourcesDirProp = Props.get("project", "res.dir");
        Prop resourcesBuildDirProp = Props.get("project", "res.build.dir");
        if ((resourcesDirProp == null) || (resourcesBuildDirProp == null)) {
            Output.print("^error^ Could not find properties 'project.res.dir' or 'project.res.build.dir'");
            System.exit(1);
        }

        File resDir = new File(resourcesDirProp.value);
        if (!resDir.exists()) {
            // nothing to copy.
            Output.print("^dbug^ No resources to copy.");
            return;
        }
        File resBuildDir = new File(resourcesBuildDirProp.value);
        if (!FileUtil.copyDir(resDir, resBuildDir)) {
            Output.print("^error^ Could not copy resources directory (%s) to resources build directory (%s).",
                    resourcesDirProp.value, resourcesBuildDirProp.value);
            System.exit(1);
        }
    }

}
