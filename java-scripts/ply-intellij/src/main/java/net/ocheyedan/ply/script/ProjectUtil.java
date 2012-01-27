package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.input.ClasspathResource;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Set;

/**
 * User: blangel
 * Date: 12/4/11
 * Time: 8:30 AM
 *
 * Updates/creates the {@literal .ipr} file for the project with the project's relevant configuration information.
 */
public class ProjectUtil {

    public static void updateProject(File projectDir) {
        Context projectContext = Context.named("project");

        String projectName = Props.getValue(projectContext, "name");
        String iprFileName = projectName + ".ipr";
        File iprFile = FileUtil.fromParts(projectDir.getPath(), iprFileName);
        Document iprDocument = IntellijUtil.readXmlDocument(new FileResource(iprFile.getPath()),
                                                            new ClasspathResource("etc/ply-intellij/templates/project.xml",
                                                                                  ProjectUtil.class.getClassLoader()));

        setJdk(iprDocument.getDocumentElement());
        Element component = IntellijUtil.findComponent(iprDocument.getDocumentElement(), "ProjectModuleManager");
        Element modules = IntellijUtil.findElement(component, "modules");
        IntellijUtil.removeElements(modules, "module");
        addModule(modules, "", projectName);
        String[] submodules = IntellijUtil.getModules();
        if (submodules != null) {
            addModules(modules, submodules);
        }
        // ensure the localRepo value is set as a path-macro so it can be referenced in creating the library-table
        // see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html
        // and http://www.jetbrains.com/idea/webhelp/path-variables.html
        String localRepoPathMacroName = IntellijUtil.setPlyLocalRepoMacro(projectName);
        // add all the dependencies from the project as well as its modules
        addLibraryTable(localRepoPathMacroName, iprDocument.getDocumentElement(), projectDir, submodules);

        Element usedPathMacros = IntellijUtil.findElement(iprDocument.getDocumentElement(), "UsedPathMacros");
        IntellijUtil.removeElements(usedPathMacros, "macro");
        Element usedMacro = IntellijUtil.createElement(usedPathMacros, "macro");
        usedMacro.setAttribute("name", localRepoPathMacroName);

        IntellijUtil.writeXmlDocument(iprFile, iprDocument);
    }

    /**
     * Adds all the project's dependencies to the 'libraryTable'
     * @param localRepoPathMacroName the path macro to use as the base of the dependency (i.e., the local repo).
     * @param root from which to find the 'libraryTable' component
     * @param projectDir the project directory to be used to resolve dependencies within {@code submodules}
     * @param submodules of the project, needed to include their dependencies as well within the library table.
     */
    private static void addLibraryTable(String localRepoPathMacroName, Element root, File projectDir, String[] submodules) {
        Element libraryTableElement = IntellijUtil.findComponent(root, "libraryTable");
        IntellijUtil.removeElements(libraryTableElement, "library");
        Set<DependencyAtom> allDeps = IntellijUtil.collectDependencies(projectDir, submodules);
        for (DependencyAtom dep : allDeps) {
            Element libraryElement = IntellijUtil.createElement(libraryTableElement, "library");
            libraryElement.setAttribute("name", "Ply: " + dep.getPropertyName() + ":" + dep.getPropertyValueWithoutTransient());
            boolean isJar = "jar".equals(dep.getSyntheticPackaging());
            // create the CLASSES element
            Element classesElement = IntellijUtil.createElement(libraryElement, "CLASSES");
            Element classesRootElement = IntellijUtil.createElement(classesElement, "root");
            String urlProtocol = dep.getSyntheticPackaging() + "://$" + localRepoPathMacroName + "$";
            String urlBase = FileUtil.pathFromParts(urlProtocol, dep.namespace, dep.name, dep.version);
            String urlValue = FileUtil.pathFromParts(urlBase, dep.getArtifactName());
            if (isJar) {
                urlValue = urlValue + "!";
            }
            urlValue = urlValue + File.separator;
            classesRootElement.setAttribute("url", urlValue);
            // create the JAVADOC element
            Element javadocElement = IntellijUtil.createElement(libraryElement, "JAVADOC");
            Element javadocRootElement = IntellijUtil.createElement(javadocElement, "root");
            DependencyAtom javadocDep = dep.withClassifier("javadoc");
            urlValue = FileUtil.pathFromParts(urlBase, javadocDep.getArtifactName());
            if (isJar) {
                urlValue = urlValue + "!";
            }
            urlValue = urlValue + File.separator;
            javadocRootElement.setAttribute("url", urlValue);
            // create the SOURCES element
            Element sourcesElement = IntellijUtil.createElement(libraryElement, "SOURCES");
            Element sourcesRootElement = IntellijUtil.createElement(sourcesElement, "root");
            DependencyAtom sourcesDep = dep.withClassifier("sources");
            urlValue = FileUtil.pathFromParts(urlBase, sourcesDep.getArtifactName());
            if (isJar) {
                urlValue = urlValue + "!";
            }
            urlValue = urlValue + File.separator;
            sourcesRootElement.setAttribute("url", urlValue);
        }
    }
    
    private static void setJdk(Element content) {
        Context intellijContext = Context.named("intellij");
        Element component = IntellijUtil.findComponent(content, "ProjectRootManager");

        String javaVersion = IntellijUtil.getJavaVersion();
        String jdkName = Props.getValue(intellijContext, "project-jdk-name");
        if (jdkName.isEmpty()) {
            jdkName = javaVersion;
        }
        component.setAttribute("project-jdk-name", jdkName);

        String jdkType = Props.getValue(intellijContext, "project-jdk-type");
        if (!jdkType.isEmpty()) {
            component.setAttribute("project-jdk-type", jdkName);
        }

        IntellijUtil.setLanguageAttribute(component, "languageLevel");

        // ply only supports >= 1.6 for compilation so default to include 1.5 features
        component.setAttribute("assert-keyword", "true");
        component.setAttribute("jdk-15", "true");

        // add target which matches java version
        component = IntellijUtil.findComponent(content, "JavacSettings");
        Element optionElement = IntellijUtil.createElement(component, "option");
        optionElement.setAttribute("ADDITIONAL_OPTIONS_STRING", "-target " + javaVersion);
    }

    private static void addModules(Element modulesElement, String[] modules) {
        for (String module : modules) {
            String name = module;
            if (module.lastIndexOf(File.separator) != -1) {
                name = name.substring(module.lastIndexOf(File.separator) + 1);
            }
            addModule(modulesElement, module, name);
        }
    }

    private static void addModule(Element modulesElement, String baseDir, String name) {
        Element moduleElement = IntellijUtil.createElement(modulesElement, "module");
        String filepath = FileUtil.pathFromParts("$PROJECT_DIR$", baseDir, name + ".iml");
        moduleElement.setAttribute("fileurl", "file://" + filepath);
        moduleElement.setAttribute("filepath", filepath);
    }

}
