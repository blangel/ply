package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFileChain;
import net.ocheyedan.ply.props.Props;

import java.io.File;
import java.util.List;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 12/3/11
 * Time: 10:55 AM
 *
 * Creates/modifies a project's {@literal Intellij} configuration files (.ipr/.iml/.iws).
 *
 * If this project is a submodule, it only creates an .iml file and adds its dependencies to the associated .ipr file.
 *
 * This script depends on the {@literal ply-dependency-manager} script having first been run.
 *
 * This implementation is influenced by the {@literal maven-idea-plugin} ({@literal http://svn.apache.org/viewvc/maven/plugins/tags/maven-idea-plugin-2.2})
 */
public class IntellijScript {

    public static void main(String[] args) {
        String plyProjectPath = Props.get("project.dir", Context.named("ply")).value();
        File projectDir = FileUtil.fromParts(plyProjectPath, "..");
        if (!projectDir.exists()) {
            Output.print("^error^ Could not determine project directory [ ^b^%s^r^ ].", projectDir.getPath());
            System.exit(1);
        }

        // execution is dependent upon whether this is a submodule.
        boolean isSubmodule = false;
        File possibleParentDir = FileUtil.fromParts(plyProjectPath, "..", "..", ".ply", "config");
        if (possibleParentDir.exists()) {
            String projectName = Props.get("name", Context.named("project")).value();
            PropFileChain parentSubmodules = Props.get(Context.named("submodules"), Props.getScope(), possibleParentDir);
            for (Prop parentSubmodule : parentSubmodules.props()) {
                if (parentSubmodule.name.equals(projectName)) {
                    isSubmodule = true;
                    break;
                }
            }
        }

        if (isSubmodule) {
            ProjectUtil.updateProjectForSubmodule(possibleParentDir, projectDir);
            ModuleUtil.updateModule(projectDir, "");
        } else {
            ProjectUtil.updateProject(projectDir);
            ModuleUtil.updateModule(projectDir, "");

            // TODO - do only if not submodules ...
            List<String> modules = IntellijUtil.getModules();
            if (modules != null) {
                for (String module : modules) {
                    ModuleUtil.updateModule(projectDir, module);
                }
            }
        }

        Output.print("Successfully created file-based Intellij structure.");
    }

}
