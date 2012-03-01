package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.input.ClasspathResource;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.props.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: blangel
 * Date: 12/4/11
 * Time: 8:30 AM
 *
 * Updates/creates the {@literal .iml} file for the project with the project's relevant configuration information.
 */
public class ModuleUtil {

    /**
     * @param projectDir the current project's root directory
     * @param owningModule the project directory of the owning module or null if this is the {@literal Intellij} project
     *                     root (i.e., contains the {@literal .ipr} file).
     */
    public static void updateModule(File projectDir, File owningModule) {
        Context projectContext = Context.named("project");
        String projectName = Props.get("name", Context.named("project")).value();
        // use the directory name to keep modules in-sync with those referenced in the .ipr file.
        String imlFileName = projectName + ".iml";
        File imlFile = FileUtil.fromParts(projectDir.getPath(), imlFileName);
        Document imlDocument = IntellijUtil.readXmlDocument(new FileResource(imlFile.getPath()),
                                                            new ClasspathResource("etc/ply-intellij/templates/module.xml",
                                                                                  ModuleUtil.class.getClassLoader()));
        String projectDirPath = FileUtil.getCanonicalPath(projectDir);
        File projectConfigDir = FileUtil.fromParts(projectDirPath, ".ply", "config");
        // nothing to do for ply
        String packaging = Props.get("packaging", projectContext, Props.getScope(), projectConfigDir).value();
        if ("war".equals(packaging)) {
            // TODO - nothing right now, pretty sure web-module is supported only in intellij-ultimate
        }

        Element component = IntellijUtil.findComponent(imlDocument.getDocumentElement(), "NewModuleRootManager");
        IntellijUtil.setLanguageAttribute(component, "LANGUAGE_LEVEL");
        component.setAttribute("inherit-compiler-output", "false");

        IntellijUtil.removeElements(component, "output");
        IntellijUtil.removeElements(component, "output-test");
        IntellijUtil.removeElements(component, "orderEntry");

        // add the inherited jdk first
        Element inheritedJdkElement = IntellijUtil.createElement(component, "orderEntry");
        inheritedJdkElement.setAttribute("type", "inheritedJdk");

        // setup the outputs/inputs
        String outputValue = Props.get("build.path", Context.named("compiler"), Props.getScope(), projectConfigDir).value();
        Element output = IntellijUtil.createElement(component, "output");
        output.setAttribute("url", "file://$MODULE_DIR$/" + outputValue);

        Element content = IntellijUtil.findElement(component, "content");
        IntellijUtil.removeElements(content, "sourceFolder");
        IntellijUtil.removeElements(content, "excludeFolder");

        content.setAttribute("url", "file://$MODULE_DIR$");
        
        String sourceFolderValue = Props.get("src.dir", projectContext, Props.getScope(), projectConfigDir).value();
        // TODO - not doing this trivial check before adding as a nice benefit of adding unequivocally is that if
        // TODO - the folder doesn't exist but is then added it is 'instantly' recognized as what it is (i.e., src/test/etc) by intellij.
        //if (FileUtil.fromParts(projectDirPath, sourceFolderValue).exists()) {
        Element sourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        sourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + sourceFolderValue);
        sourceFolder.setAttribute("isTestSource", "false");

        String resourceFolderValue = Props.get("res.dir", projectContext, Props.getScope(), projectConfigDir).value();
        Element resourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        resourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + resourceFolderValue);
        resourceFolder.setAttribute("isTestSource", "false");

        String excludeFolderValue = Props.get("build.dir", projectContext, Props.getScope(), projectConfigDir).value();
        Element excludeFolder = IntellijUtil.createElement(content, "excludeFolder");
        excludeFolder.setAttribute("url", "file://$MODULE_DIR$/" + excludeFolderValue);

        // setup the test outputs/inputs
        String testOutputValue = getTestPropValue("build.path", Context.named("compiler"), projectConfigDir);
        Element testOutput = IntellijUtil.createElement(component, "output-test");
        testOutput.setAttribute("url", "file://$MODULE_DIR$/" + testOutputValue);

        String testSourceFolderValue = getTestPropValue("src.dir", projectContext, projectConfigDir);
        Element testSourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        testSourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + testSourceFolderValue);
        testSourceFolder.setAttribute("isTestSource", "true");

        String testResourceFolderValue = getTestPropValue("res.dir", projectContext, projectConfigDir);
        Element testResourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        testResourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + testResourceFolderValue);
        testResourceFolder.setAttribute("isTestSource", "true");

        // add the canned order-entry elements
        Element sourceFolderElement = IntellijUtil.createElement(component, "orderEntry");
        sourceFolderElement.setAttribute("type", "sourceFolder");
        sourceFolderElement.setAttribute("forTests", "false");

        // add the dependencies as order-entry elements
        addDependencies(component, projectDir, owningModule);

        IntellijUtil.writeXmlDocument(imlFile, imlDocument);
    }

    /**
     * Adds all test and {@link Props#getScope()} dependencies as {@literal orderEntry} elements; for instance:
     * {@literal
     * <orderEntry type="module" module-name="ply-util" />
     * <orderEntry type="library" name="Maven: commons-lang:commons-lang:2.6" level="project" />
     * <orderEntry type="library" scope="TEST" name="Maven: junit:junit:4.10" level="project" />
     * }
     * @param to is the element into which to insert {@literal orderEntry} elements
     * @param projectDir is the base directory from which the script was executed
     * @param owningModule the directory of the owning module (the {@literal Intellij} root project) or null
     *                     if this is the root project
     */
    private static void addDependencies(Element to, File projectDir, File owningModule) {
        Scope defaultScope = Scope.named(Props.get("scope", Context.named("ply")).value());
        Scope testScope = Scope.named("test");
        File projectConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(projectDir), ".ply", "config");
        List<String> modules;
        if (owningModule != null) {
            File owningModuleConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(owningModule), ".ply", "config");
            modules = IntellijUtil.getModules(owningModuleConfigDir);
        } else {
            modules = IntellijUtil.getModules();
        }
        Set<DependencyAtom> excludes = new HashSet<DependencyAtom>();
        excludes.add(getSelf());
        excludes.addAll(addDependencies(to, projectConfigDir, defaultScope, "", modules, excludes));
        addDependencies(to, projectConfigDir, testScope, "TEST", modules, excludes);
    }
    
    private static DependencyAtom getSelf() {
        DependencyAtom self = Deps.getProjectDep();
        // the resolved-dep file stores all dependencies in a fully-qualified path name, which means the artifactName
        // will be explicitly set, therefore, explicitly set on self so that exclusions work properly
        return new DependencyAtom(self.namespace, self.name, self.version, self.getArtifactName(), self.transientDep);
    }

    /**
     * Adds all {@code scope} dependencies as {@literal orderEntry} elements; for instance:
     * {@literal
     * <orderEntry type="module" module-name="ply-util" />
     * <orderEntry type="library" name="Maven: commons-lang:commons-lang:2.6" level="project" />
     * <orderEntry type="library" scope="TEST" name="Maven: junit:junit:4.10" level="project" />
     * }
     * @param to is the element into which to insert {@literal orderEntry} elements
     * @param projectConfigDir the project configuration directory from which to get resolved dependencies.
     * @param scope of the dependencies to load
     * @param intellijScope if not the emptry string then a {@literal scope} attribute will be added to
     *        the {@literal orderEntry} element
     * @param modules the array of modules for the project
     * @param excludes set of dependencies to exclude from addition
     * @return the list of dependencies added
     */
    private static Set<DependencyAtom> addDependencies(Element to, File projectConfigDir, Scope scope, String intellijScope, 
                                                       List<String> modules, Set<DependencyAtom> excludes) {
        Set<DependencyAtom> dependencies = IntellijUtil.collectDependencies(projectConfigDir, scope);
        for (DependencyAtom dep : dependencies) {
            if (excludes.contains(dep)) {
                continue;
            }
            Element orderEntry = IntellijUtil.createElement(to, "orderEntry");
            if (isModule(dep, modules)) {
                orderEntry.setAttribute("type", "module");
                orderEntry.setAttribute("module-name", dep.name);
            } else {
                orderEntry.setAttribute("type", "library");
                orderEntry.setAttribute("name", "Ply: " + dep.getPropertyName() + ":" + dep.getPropertyValueWithoutTransient());
                orderEntry.setAttribute("level", "project");
            }
            if (!intellijScope.isEmpty()) {
                orderEntry.setAttribute("scope", intellijScope);
            }
        }
        return dependencies;
    }

    /**
     * @param dep to check
     * @param modules the project's module from which to check
     * @return true if {@code dep}'s {@link DependencyAtom#name} is within {@code modules}
     */
    private static boolean isModule(DependencyAtom dep, List<String> modules) {
        if (modules == null) {
            return false;
        }
        for (String module : modules) {
            String moduleName = module;
            if (module.endsWith("/") || module.endsWith("\\")) {
                moduleName = moduleName.substring(0, moduleName.length() - 1);
            }
            if (moduleName.lastIndexOf(File.separator) != -1) {
                moduleName = moduleName.substring(module.lastIndexOf(File.separator) + 1);
            }
            if (moduleName.equals(dep.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Since this script is being invoked, property values are already resolved.  We need to get the values set for
     * the test scope but we are not running in the test scope.  This method explicitly looks up property values
     * from within the {@code projectConfigDir} and if not found then within the {@link PlyUtil#SYSTEM_CONFIG_DIR}
     * @param propertyName name of the property to return
     * @param context of the property file to retrieve (for scope {@literal test}).
     * @param projectConfigDir the configuration directory of the local project
     * @return the value of {@code propertyName} in {@code context} for scope {@literal test} or the empty string
     *         if it couldn't be found in either the local or system directory.
     */
    private static String getTestPropValue(String propertyName, Context context, File projectConfigDir) {
        String propertyFileName = context.name + ".test.properties";
        String localPath = FileUtil.pathFromParts(FileUtil.getCanonicalPath(projectConfigDir), propertyFileName);
        if (new File(localPath).exists()) {
            PropFile localPropFile = PropFiles.load(localPath, false,  true);
            if ((localPropFile != null) && localPropFile.contains(propertyName)) {
                return localPropFile.get(propertyName).value();
            }    
        }
        String systemPath = FileUtil.pathFromParts(FileUtil.getCanonicalPath(PlyUtil.SYSTEM_CONFIG_DIR), propertyFileName);
        if (new File(systemPath).exists()) {
            PropFile systemPropFile = PropFiles.load(systemPath, false, true);
            if (systemPropFile != null) {
                return systemPropFile.get(propertyName).value();
            }
        }
        return "";
    }

}
