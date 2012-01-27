package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.util.Collection;

/**
 * User: blangel
 * Date: 12/3/11
 * Time: 10:55 AM
 *
 * Creates/modifies a project's {@literal Intellij} configuration files (.ipr/.iml/.iws).
 *
 * This implementation is a modified version of the {@literal maven-idea-plugin} ({@literal http://svn.apache.org/viewvc/maven/plugins/tags/maven-idea-plugin-2.2})
 */
public class IntellijScript {

    public static void main(String[] args) {
        String plyProjectPath = Props.getValue(Context.named("ply"), "project.dir");
        File projectDir = FileUtil.fromParts(plyProjectPath, "..");
        if (!projectDir.exists()) {
            Output.print("^error^ Could not determine project directory [ ^b^%s^r^ ].", projectDir.getPath());
            System.exit(1);
        }

        // skip execution if this is a known submodule.
        File possibleParentDir = FileUtil.fromParts(plyProjectPath, "..", "..", ".ply", "config");
        if (possibleParentDir.exists()) {
            String projectName = Props.getValue(Context.named("project"), "name");
            Collection<Prop> parentSubmodules = Props.getForceResolution(Context.named("submodules"), possibleParentDir);
            for (Prop parentSubmodule : parentSubmodules) {
                if (parentSubmodule.name.equals(projectName)) {
                    Output.print("^info^ Skipping ^b^%s^r^ as it is a module for its parent directory.", projectName);
                    return;
                }
            }
        }

        ProjectUtil.updateProject(projectDir);
//        WorkspaceUtil.updateWorkspace(projectDir);
        ModuleUtil.updateModule(projectDir, "");

        String[] modules = IntellijUtil.getModules();
        if (modules != null) {
            for (String module : modules) {
                ModuleUtil.updateModule(projectDir, module);
            }
        }
        Output.print("Successfully created file-based Intellij structure.");
    }

}
