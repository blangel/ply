package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.input.ClasspathResource;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;
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
     * @param subdirectory the name of the sub-directory (submodule) which to create the {@literal .iml} file
     *                     if this is the empty string it is assumed to be for the current project.
     */
    public static void updateModule(File projectDir, String subdirectory) {
        Context projectContext = Context.named("project");
        File projectRootDir = (subdirectory.isEmpty() ? projectDir : FileUtil.fromParts(FileUtil.getCanonicalPath(projectDir), subdirectory));
        String projectName = (subdirectory.isEmpty() ? Props.getValue(Context.named("project"), "name") : projectRootDir.getName());
        // use the directory name to keep modules in-sync with those referenced in the .ipr file.
        String imlFileName = projectName + ".iml";
        File imlFile = FileUtil.fromParts(projectRootDir.getPath(), imlFileName);
        Document imlDocument = IntellijUtil.readXmlDocument(new FileResource(imlFile.getPath()),
                                                            new ClasspathResource("etc/ply-intellij/templates/module.xml",
                                                                                  ModuleUtil.class.getClassLoader()));
        // nothing to do for ply
        String packaging = getPropValue(projectDir, subdirectory, projectContext, "packaging");
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
        Element output = IntellijUtil.createElement(component, "output");
        output.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory,
                                    Context.named("compiler"), "build.path"));

        Element content = IntellijUtil.findElement(component, "content");
        IntellijUtil.removeElements(content, "sourceFolder");
        IntellijUtil.removeElements(content, "excludeFolder");

        content.setAttribute("url", "file://$MODULE_DIR$");
        Element sourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        sourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory, projectContext,
                "src.dir"));
        sourceFolder.setAttribute("isTestSource", "false");
        Element resourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        resourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory,
                projectContext, "res.dir"));
        resourceFolder.setAttribute("isTestSource", "false");
        Element excludeFolder = IntellijUtil.createElement(content, "excludeFolder");
        excludeFolder.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory,
                projectContext, "build.dir"));

        // setup the test outputs/inputs
        Element testOutput = IntellijUtil.createElement(component, "output-test");
        testOutput.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory,
                                    Context.named("compiler"), Scope.named("test"), "build.path"));

        Element testSourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        testSourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory, projectContext,
                Scope.named("test"), "src.dir"));
        testSourceFolder.setAttribute("isTestSource", "true");
        Element testResourceFolder = IntellijUtil.createElement(content, "sourceFolder");
        testResourceFolder.setAttribute("url", "file://$MODULE_DIR$/" + getPropValue(projectDir, subdirectory,
                projectContext, Scope.named("test"), "res.dir"));
        testResourceFolder.setAttribute("isTestSource", "true");

        // add the canned order-entry elements
        Element sourceFolderElement = IntellijUtil.createElement(component, "orderEntry");
        sourceFolderElement.setAttribute("type", "sourceFolder");
        sourceFolderElement.setAttribute("forTests", "false");

        // add the dependencies as order-entry elements
        addDependencies(component, projectDir, subdirectory);

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
     * @param subdirectory the sub-directory for which the module file is being made or the empty string
     *        if this is the module file for the project itself
     */
    private static void addDependencies(Element to, File projectDir, String subdirectory) {
        Scope scope = Props.getScope();
        Scope testScope = Scope.named("test");
        String canonicalProjectDirPath = FileUtil.getCanonicalPath(projectDir);
        String[] modules = IntellijUtil.getModules();
        Set<DependencyAtom> excludes = new HashSet<DependencyAtom>();
        excludes.addAll(addDependencies(to, canonicalProjectDirPath, subdirectory, scope, "", modules, Collections.<DependencyAtom>emptySet()));
        addDependencies(to, canonicalProjectDirPath, subdirectory, testScope, "TEST", modules, excludes);
    }

    /**
     * Adds all {@code scope} dependencies as {@literal orderEntry} elements; for instance:
     * {@literal
     * <orderEntry type="module" module-name="ply-util" />
     * <orderEntry type="library" name="Maven: commons-lang:commons-lang:2.6" level="project" />
     * <orderEntry type="library" scope="TEST" name="Maven: junit:junit:4.10" level="project" />
     * }
     * @param to is the element into which to insert {@literal orderEntry} elements
     * @param canonicalProjectDirPath is the base directory from which the script was executed in canonical form
     * @param subdirectory the sub-directory for which the module file is being made or the empty string
     *        if this is the module file for the project itself
     * @param scope for which to extract dependencies
     * @param intellijScope if not the emptry string then a {@literal scope} attribute will be added to
     *        the {@literal orderEntry} element
     * @param modules the array of modules for the project
     * @param excludes set of dependencies to exclude from addition
     * @return the list of dependencies added
     */
    private static List<DependencyAtom> addDependencies(Element to, String canonicalProjectDirPath, String subdirectory,
                                        Scope scope, String intellijScope, String[] modules, Set<DependencyAtom> excludes) {
        List<DependencyAtom> dependencies;
        if (subdirectory.isEmpty() && Scope.Default.equals(scope)) {
            dependencies = Deps.parse(Props.get(Context.named("dependencies")));
        } else {
            File submoduleConfigDir = FileUtil.fromParts(canonicalProjectDirPath, subdirectory, ".ply", "config");
            dependencies = Deps.parse(Props.getForceResolution(Context.named("dependencies"), submoduleConfigDir, scope));
        }
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
    private static boolean isModule(DependencyAtom dep, String[] modules) {
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
     * Calls {@link Props#getValue(Context, String)} with {@code context} and {@code name} if {@code subdirectory}
     * is the empty-string.  Otherwise determines the current config-dir based on {@code projectDir} and {@code subdirectory}
     * and calls {@link Props#getValue(Context, String, File, Scope)} with {@code context}
     * {@code named} the resolved config directory (composed from {@code projectDir} and {@code subdirectory}) and the
     * value of {@link Props#getScope()}
     * @param projectDir of the project from which the script was run
     * @param subdirectory of the submodule (relative to {@code projectDir}) or the empty-string if the module
     *                     in question is that associated with {@code projectDir} directly
     * @param context from which to retrieve the property value
     * @param named the name of the property for which to retrieve the property value
     * @return the property value {@code named} in {@code context}
     */
    private static String getPropValue(File projectDir, String subdirectory, Context context, String named) {
        return getPropValue(projectDir, subdirectory, context, Props.getScope(), named);
    }

    /**
     * Calls {@link Props#getValue(Context, String)} with {@code context} and {@code name} if {@code subdirectory}
     * is the empty-string.  Otherwise determines the current config-dir based on {@code projectDir} and {@code subdirectory}
     * and calls {@link Props#getValue(Context, String, File, Scope)} with {@code context}
     * {@code named} the resolved config directory (composed from {@code projectDir} and {@code subdirectory}) and the
     * value of {@code scope}.
     * Note, if {@code scope} is {@literal test} then resolution is done against the property files as we need
     * to extract the test values of properties even if this is for the current project directory.
     * @param projectDir of the project from which the script was run
     * @param subdirectory of the submodule (relative to {@code projectDir}) or the empty-string if the module
     *                     in question is that associated with {@code projectDir} directly
     * @param context from which to retrieve the property value
     * @param scope for which to retrieve the property value
     * @param named the name of the property for which to retrieve the property value
     * @return the property value {@code named} in {@code context}
     */
    private static String getPropValue(File projectDir, String subdirectory, Context context, Scope scope, String named) {
        if (subdirectory.isEmpty() && !Scope.named("test").equals(scope)) {
            return Props.getValue(context, named);
        }
        String canonicalProjectDirPath = FileUtil.getCanonicalPath(projectDir);
        File submoduleConfigDir = FileUtil.fromParts(canonicalProjectDirPath, subdirectory, ".ply", "config");
        return Props.getValueForceResolution(context, named, submoduleConfigDir, scope);
    }

}
