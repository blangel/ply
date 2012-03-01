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

        File owningModuleDir = getOwningModule(projectDir);
        if (owningModuleDir != null) {
            ProjectUtil.updateProjectForSubmodule(owningModuleDir, projectDir);
        } else {
            ProjectUtil.updateProject(projectDir);
        }
        ModuleUtil.updateModule(projectDir, owningModuleDir);

        Output.print("Successfully created file-based Intellij structure.");
    }

    /**
     * @param projectDir for which to determine if it is a submodule of an owning module.
     * @return the owning module's directory if {@code projectDir} is a submodule, null otherwise.
     */
    private static File getOwningModule(File projectDir) {
        boolean isSubmodule = false;
        File parentDir = projectDir;
        String moduleName = "", projectDirPath = FileUtil.getCanonicalPath(projectDir);
        // try at least 3 directories upward - TODO - best way to actually figure out the owning-project
        int i = 0;
        while (!isSubmodule && (i++ < 3)) {
            File possibleParentConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(parentDir), "..", ".ply", "config");
            parentDir = FileUtil.fromParts(FileUtil.getCanonicalPath(possibleParentConfigDir), "..", "..");
            try {
                moduleName = projectDirPath.substring(FileUtil.getCanonicalPath(parentDir).length() + 1);
            } catch (IndexOutOfBoundsException ioobe) {
                break; // we hit the root directory...abort
            }
            if (possibleParentConfigDir.exists()) {
                PropFileChain parentSubmodules = Props.get(Context.named("submodules"), Props.getScope(), possibleParentConfigDir);
                for (Prop parentSubmodule : parentSubmodules.props()) {
                    if (parentSubmodule.name.equals(moduleName)) {
                        isSubmodule = true;
                        break;
                    }
                }
            }
        }
        return (isSubmodule ? parentDir : null);
    }

}
