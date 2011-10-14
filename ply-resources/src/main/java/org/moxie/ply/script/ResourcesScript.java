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
 * Simply copies {@literal project[#scope]#res.dir} to {@literal project[#scope]#res.build.dir} priming the resources
 * for filtering/packaging/etc.
 * This script takes one optional parameter which is the scope.  By convention it is prefixed with '--'
 */
public class ResourcesScript {

    public static void main(String[] args) {
        String scope = "";
        if ((args.length == 1) && args[0].startsWith("--")) {
            scope = args[0].substring(2);
        }

        Prop resourcesDirProp = Props.get("project", scope, "res.dir");
        Prop resourcesBuildDirProp = Props.get("project", scope, "res.build.dir");
        if ((resourcesDirProp == null) || (resourcesBuildDirProp == null)) {
            String scopeParam = (scope.isEmpty() ? "" : "#" + scope);
            Output.print("^error^ Could not find properties 'project%s#res.dir' or 'project%s#res.build.dir'", scopeParam, scopeParam);
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
