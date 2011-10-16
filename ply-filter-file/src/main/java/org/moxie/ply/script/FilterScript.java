package org.moxie.ply.script;

import org.moxie.ply.Output;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;
import org.moxie.ply.props.Scope;

import java.io.File;
import java.util.Map;

/**
 * User: blangel
 * Date: 10/15/11
 * Time: 11:13 AM
 *
 * Filters files from {@literal project[#scope]#build.dir} in place.  Only those files matching ant-style patterns
 * specified in {@literal filter-files[.scope].properties} (thus the properties have context
 * {@literal [scope.]filter-files}) are filtered; i.e., nothing is filtered by default.
 * The property name is interpreted as the ant-style wildcard pattern to match and the value is either null, in which
 * case the pattern is considered to represent a file or set of files to be filtered or 'NOT' which means the
 * pattern is considered to represent a file or set of files to explicitly exclude from filtering.  By default everything
 * is excluded so the explicit excludes are to limit the set matched by other inclusions.
 *
 * All files are filtered in place and the ant-style wildcard pattern is always relative to {@literal project[#scope]#buid.dir}
 * thus any file reachable from {@literal project[#scope]#buid.dir} can be filtered (which includes resources
 * or even source files before compilation if they were first copied into the build directory).
 * TODO - should, by default, ply copy source files to the build dir and then compile, allowing filtering easily?
 *
 * The files included to be filtered are filtered for unix-style property placeholders; i.e., ${xxxx}, where 'xxxx'
 * is the property name for which to filter.  The property names available to be used in filtering are all those
 * available to {@literal ply} which includes all context properties as well as environmental variables.  For instance,
 * if there is a {@literal $\{compiler.buildDir\}} then the property {@literal compiler[#scope].buildDir} is looked
 * up and its value is used in filtering.
 * 
 * This script takes one optional parameter which is the scope.  By convention it is prefixed with '--'
 */
public class FilterScript {

    public static void main(String[] args) {
        String scope = "";
        if ((args.length == 1) && args[0].startsWith("--")) {
            scope = args[0].substring(2);
        }

        Prop buildDirProp = Props.get("project", scope, "build.dir");
        if (buildDirProp == null) {
            Output.print("^error^ Could not find project%s#build.dir property.", new Scope(scope).envSuffix);
            System.exit(1);
        }
        Map<String, Prop> filterFiles = Props.getProperties("filter-files", scope, false);
        if (filterFiles.isEmpty()) {
            Output.print("^dbug^ No filter sets specified, skipping filtering.");
            return;
        }
        File buildDir = new File(buildDirProp.value);
        

    }

}
