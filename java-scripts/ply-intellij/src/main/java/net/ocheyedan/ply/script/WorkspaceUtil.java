package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.input.ClasspathResource;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import org.w3c.dom.Document;

import java.io.File;

/**
 * User: blangel
 * Date: 12/3/11
 * Time: 2:24 PM
 *
 * Updates/creates the {@literal .iws} file for the project with the project's relevant configuration information.
 */
public class WorkspaceUtil {

    public static void updateWorkspace(File projectDir) {

        String projectName = Props.get("name", Context.named("project")).value();
        String iwsFileName = projectName + ".iws";
        File iwsFile = FileUtil.fromParts(projectDir.getPath(), iwsFileName);
        Document iwsDocument = IntellijUtil.readXmlDocument(new FileResource(iwsFile.getPath()),
                                                            new ClasspathResource("etc/ply-intellij/templates/workspace.xml",
                                                                                  WorkspaceUtil.class.getClassLoader()));
        // nothing to do for ply

        IntellijUtil.writeXmlDocument(iwsFile, iwsDocument);
    }

}
