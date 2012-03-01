package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.input.ClasspathResource;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.List;
import java.util.Set;

/**
 * User: blangel
 * Date: 12/4/11
 * Time: 8:30 AM
 *
 * Updates/creates the {@literal .ipr} file for the project with the project's relevant configuration information.
 */
public final class ProjectUtil {

    /**
     * Information pertaining to an {@literal .ipr} document.
     */
    private final static class IprDocument {
        private final Document iprXmlDocument;
        private final String projectName;
        private final File iprXmlFile;
        private IprDocument(Document iprXmlDocument, String projectName, File iprXmlFile) {
            this.iprXmlDocument = iprXmlDocument;
            this.projectName = projectName;
            this.iprXmlFile = iprXmlFile;
        }
    }

    /**
     * Creates or updates the {@literal .ipr} file located within {@code projectDir}
     * @param projectDir in which to create or update the {@literal .ipr} file.
     */
    public static void updateProject(File projectDir) {
        String projectName = Props.get("name", Context.named("project")).value();
        IprDocument iprDocument = getIprDocument(projectDir, projectName);

        setJdk(iprDocument.iprXmlDocument.getDocumentElement());
        Element component = IntellijUtil.findComponent(iprDocument.iprXmlDocument.getDocumentElement(), "ProjectModuleManager");
        Element modules = IntellijUtil.findElement(component, "modules");
        IntellijUtil.removeElements(modules, "module");
        addModule(modules, "", iprDocument.projectName);
        List<String> submodules = IntellijUtil.getModules();
        addModules(modules, submodules);
        // add all the dependencies from the project's {@literal resolved-deps.properties} file
        File projectConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(projectDir), ".ply", "config");
        updateProject(iprDocument, projectConfigDir);
    }

    /**
     * Updates the {@literal .ipr} file associated with {@code owningModuleDir} by adding the library table entries
     * for all resolved dependencies of {@code submoduleProjectDir}
     * @param owningModuleDir the directory in which the {@literal .ipr} file is located
     * @param submoduleProjectDir the directory in which the {@literal .iml} file is located from which to 
     *                            extract the resolved dependencies in order to add to the {@code projectDir}'s 
     *                            {@literal .ipr} file
     */
    public static void updateProjectForSubmodule(File owningModuleDir, File submoduleProjectDir) {
        // get the owningModuleDir configuration directory
        File owningModuleConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(owningModuleDir), ".ply", "config");
        String projectName = Props.get("name", Context.named("project"), Props.getScope(), owningModuleConfigDir).value();
        IprDocument iprDocument = getIprDocument(owningModuleDir, projectName);
        // add all the dependencies from the project's {@literal resolved-deps.properties} file
        File submoduleProjectConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(submoduleProjectDir), ".ply", "config");
        updateProject(iprDocument, submoduleProjectConfigDir);
    }
    
    private static void updateProject(IprDocument iprDocument, File projectConfigDir) {
        // ensure the localRepo value is set as a path-macro so it can be referenced in creating the library-table
        // see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html
        // and http://www.jetbrains.com/idea/webhelp/path-variables.html
        String localRepoPathMacroName = IntellijUtil.setPlyLocalRepoMacro(iprDocument.projectName);
        addLibraryTable(localRepoPathMacroName, iprDocument.iprXmlDocument.getDocumentElement(), projectConfigDir);

        Element usedPathMacros = IntellijUtil.findElement(iprDocument.iprXmlDocument.getDocumentElement(),
                "UsedPathMacros");
        Element usedMacro = IntellijUtil.findElement(usedPathMacros, "macro", localRepoPathMacroName);
        usedMacro.setAttribute("name", localRepoPathMacroName);

        IntellijUtil.writeXmlDocument(iprDocument.iprXmlFile, iprDocument.iprXmlDocument);
    }

    /**
     * @param projectDir from which to return the {@literal .ipr} file.
     * @param projectName the name of the project which is used as the file name of the {@literal .ipr} file.                  
     * @return the corresponding project's {@literal .ipr} file.
     */
    private static IprDocument getIprDocument(File projectDir, String projectName) {
        String iprFileName = projectName + ".ipr";
        File iprFile = FileUtil.fromParts(projectDir.getPath(), iprFileName);
        Document iprDocument = IntellijUtil.readXmlDocument(new FileResource(iprFile.getPath()),
                new ClasspathResource("etc/ply-intellij/templates/project.xml",
                        ProjectUtil.class.getClassLoader()));
        return new IprDocument(iprDocument, projectName, iprFile);
    }

    /**
     * Adds all the project's dependencies to the 'libraryTable'
     * @param localRepoPathMacroName the path macro to use as the base of the dependency (i.e., the local repo).
     * @param root from which to find the 'libraryTable' component
     * @param projectConfigDir the project's configuration directory to be used to resolve dependencies
     */
    private static void addLibraryTable(String localRepoPathMacroName, Element root, File projectConfigDir) {
        Element libraryTableElement = IntellijUtil.findComponent(root, "libraryTable");
        Set<DependencyAtom> allDeps = IntellijUtil.collectDependencies(projectConfigDir);
        allDeps.addAll(IntellijUtil.collectDependencies(projectConfigDir, Scope.named("test"))); // add test scoped deps as well
        for (DependencyAtom dep : allDeps) {
            final String libraryName = "Ply: " + dep.getPropertyName() + ":" + dep.getPropertyValueWithoutTransient();
            final Element libraryElement = IntellijUtil.findElement(libraryTableElement, "library", libraryName);
            libraryElement.setAttribute("name", libraryName);
            final boolean isJar = "jar".equals(dep.getSyntheticPackaging());
            final String urlProtocol = dep.getSyntheticPackaging() + "://$" + localRepoPathMacroName + "$";
            final String urlBase = FileUtil.pathFromParts(urlProtocol, dep.namespace, dep.name, dep.version);
            // create the CLASSES element
            if (!IntellijUtil.hasElement(libraryElement, "CLASSES")) {
                Element classesElement = IntellijUtil.createElement(libraryElement, "CLASSES");
                Element classesRootElement = IntellijUtil.createElement(classesElement, "root");
                String urlValue = FileUtil.pathFromParts(urlBase, dep.getArtifactName());
                if (isJar) {
                    urlValue = urlValue + "!";
                }
                urlValue = urlValue + File.separator;
                classesRootElement.setAttribute("url", urlValue);    
            }
            // create the JAVADOC element
            if (!IntellijUtil.hasElement(libraryElement, "JAVADOC")) {
                Element javadocElement = IntellijUtil.createElement(libraryElement, "JAVADOC");
                Element javadocRootElement = IntellijUtil.createElement(javadocElement, "root");
                DependencyAtom javadocDep = dep.withClassifier("javadoc");
                String urlValue = FileUtil.pathFromParts(urlBase, javadocDep.getArtifactName());
                if (isJar) {
                    urlValue = urlValue + "!";
                }
                urlValue = urlValue + File.separator;
                javadocRootElement.setAttribute("url", urlValue);    
            }
            // create the SOURCES element
            if (!IntellijUtil.hasElement(libraryElement, "SOURCES")) {
                Element sourcesElement = IntellijUtil.createElement(libraryElement, "SOURCES");
                Element sourcesRootElement = IntellijUtil.createElement(sourcesElement, "root");
                DependencyAtom sourcesDep = dep.withClassifier("sources");
                String urlValue = FileUtil.pathFromParts(urlBase, sourcesDep.getArtifactName());
                if (isJar) {
                    urlValue = urlValue + "!";
                }
                urlValue = urlValue + File.separator;
                sourcesRootElement.setAttribute("url", urlValue);       
            }
        }
    }
    
    private static void setJdk(Element content) {
        Context intellijContext = Context.named("intellij");
        Element component = IntellijUtil.findComponent(content, "ProjectRootManager");

        String javaVersion = IntellijUtil.getJavaVersion();
        String jdkName = Props.get("project-jdk-name", intellijContext).value();
        if (jdkName.isEmpty()) {
            jdkName = javaVersion;
        }
        component.setAttribute("project-jdk-name", jdkName);

        String jdkType = Props.get("project-jdk-type", intellijContext).value();
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

    private static void addModules(Element modulesElement, List<String> modules) {
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

    private ProjectUtil() { }

}
