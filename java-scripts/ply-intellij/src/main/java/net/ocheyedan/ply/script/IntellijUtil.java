package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.input.FileResource;
import net.ocheyedan.ply.input.Resource;
import net.ocheyedan.ply.jna.JnaUtil;
import net.ocheyedan.ply.props.*;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 12/3/11
 * Time: 2:27 PM
 *
 * Utility to deal with {@literal Intellij} specific files/apis/etc.
 */
public class IntellijUtil {

    /**
     * @return the {@literal submodules} context values or an empty list.
     */
    public static List<String> getModules() {
        Scope scope = getSubmodulesScope();
        return getModules(Props.get(Context.named("submodules"), scope), Props.get("name", Context.named("project")).value());
    }

    /**
     * @param projectConfigDir the project configuration directory which to retrieve the {@literal submodules} values.
     * @return the {@literal submodules} context values for the project at configuration directory {@code projectConfigDir}
     *         or an empty list.
     */
    public static List<String> getModules(File projectConfigDir) {
        String projectName = Props.get("name", Context.named("project"), Props.getScope(), projectConfigDir).value();
        return getModules(Props.get(Context.named("submodules"), getSubmodulesScope(), projectConfigDir), projectName);
    }
    
    private static List<String> getModules(PropFileChain submodules, String projectName) {
        List<String> submoduleNames = new ArrayList<String>();
        for (Prop submodule : submodules.props()) {
            if ("exclude".equals(submodule.value())) {
                continue;
            }
            submoduleNames.add(submodule.name); // TODO - recursively get submodules' submodules, if any
        }
        if (!projectName.isEmpty()) {
            submoduleNames.add(projectName);
        }
        return submoduleNames;
    }

    /**
     * Ensures that the ply local repo is set correctly within the Intellij project config/options/path.macros.xml file
     * for {@code projectName}.
     * @param projectName of the project for which to set the ply local-repo macro.
     * @return the macro name to use for {@code projectName} or null if the macro could not be set (i.e., cannot find
     *         user's intellij dir or there's a permissions' issue).
     * @see {@literal http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html}
     * @see {@literal http://www.jetbrains.com/idea/webhelp/path-variables.html#pv}
     */
    public static String setPlyLocalRepoMacro(String projectName) {
        // first, get the localRepo Prop
        String localRepoValue = Props.get("localRepo", Context.named("depmngr")).value();
        String systemLocalRepoValue = getSystemLocalRepo();
        systemLocalRepoValue = Filter.filter(systemLocalRepoValue, Context.named("depmngr"), systemLocalRepoValue, Props.get());
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoValue);
        String plyRepoPathMacroName = "PLY_REPO";
        // if the localRepo was set locally then append projectName
        if (!localRepoValue.equals(systemLocalRepoValue)) {
            plyRepoPathMacroName = plyRepoPathMacroName + "_" + projectName;
        }
        // second, get the intellij-directory
        File intellijDir = getIntellijDirectory();
        if ((intellijDir == null) || !intellijDir.exists()) {
            Output.print("^error^ Could not determine your user's Intellij configuration directory.");
            Output.print("^error^ see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html#topicSeeAlsoTopicsList");
            Output.print("^error^ going to continue by assuming existence of intellij path-macro of PLY_REPO=%s", localRepo.getPropertyName());
            return "PLY_REPO";
        }
        File intellijConfigOptionsDir;
        // @see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html
        if (JnaUtil.getOperatingSystem() == JnaUtil.Os.OSX) {
            intellijConfigOptionsDir = FileUtil.fromParts(intellijDir.getPath(), "options");
        } else {
            intellijConfigOptionsDir = FileUtil.fromParts(intellijDir.getPath(), "config", "options");
        }
        if (!intellijConfigOptionsDir.exists()) {
            Output.print("^error^ Found something which looks like an Intellij configuration directory (%s)", intellijDir.getPath());
            Output.print("^error^ However it does not contain the ^b^options^r^ sub-directory.");
            Output.print("^error^ see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html#topicSeeAlsoTopicsList");
            Output.print("^error^ going to continue by assuming existence of intellij path-macro of PLY_REPO=%s", localRepo.getPropertyName());
            return "PLY_REPO";
        }
        // third, get the path macro from the document
        File pathMacrosXmlFile = FileUtil.fromParts(intellijConfigOptionsDir.getPath(), "path.macros.xml");
        Document pathMacrosXmlFileDoc = readXmlDocument(new FileResource(pathMacrosXmlFile.getPath()), null);
        if (pathMacrosXmlFileDoc == null) {
            Output.print("^error^ Could not open the ^b^path.macros.xml^r^ file within ^b^%s^r^", intellijConfigOptionsDir.getPath());
            Output.print("^error^ going to continue by assuming existence of intellij path-macro of PLY_REPO=%s", localRepo.getPropertyName());
            return "PLY_REPO";
        }
        Element componentElement = findComponent(pathMacrosXmlFileDoc.getDocumentElement(), "PathMacrosImpl");
        // get the path macro value so that we can find an existing macro by name first and if none match then
        // by value.
        String localRepoPath = localRepo.getPropertyName();
        if (localRepoPath.startsWith("file://")) {
            localRepoPath = localRepoPath.substring(7);
        }
        // find/create the macro element as necessary
        Element plyRepoPathMacroElement = findMacroElementByNameAndValue(componentElement, plyRepoPathMacroName, localRepoPath);
        // fourth, set the path macro value (even if already set as the user is choosing to run this script so likely
        // the value needs to be updated or the value is already the same anyway).
        plyRepoPathMacroElement.setAttribute("value", localRepoPath);
        writeXmlDocument(pathMacrosXmlFile, pathMacrosXmlFileDoc);
        return plyRepoPathMacroName;
    }

    /**
     * Similar to {@link #findElement(org.w3c.dom.Element, String, String)} except if the {@literal name} attribute with
     * value {@code macroName} is not found this method will also check for a {@literal value} attribute with value equal
     * to {@code macroValue} and only if neither are found will this method create the element.
     * @param componentElement the {@literal PathMacrosImpl} component
     * @param macroName value of the {@literal macro} tag's {@literal name} attribute to match
     * @param macroValue value of the {@literal macro} tag's {@literal value} attribute to match
     * @return the found {@link Element} or the created element if one could not be found.
     */
    private static Element findMacroElementByNameAndValue(Element componentElement, String macroName, String macroValue) {
        NodeList children = componentElement.getElementsByTagName("macro");
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element childElement = (Element) child;
            if (childElement.hasAttribute("name") && macroName.equals(childElement.getAttribute("name"))) {
                return childElement;
            }
        }
        // still not found, search by value
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element childElement = (Element) child;
            if (childElement.hasAttribute("value") && macroValue.equals(childElement.getAttribute("value"))) {
                return childElement;
            }
        }
        // still not found, create it
        Element createdElement = createElement(componentElement, "macro");
        createdElement.setAttribute("name", macroName);
        return createdElement;
    }

    /**
     * @return the system value for the {@literal depmngr.localRepo} property for the default scope
     */
    private static String getSystemLocalRepo() {
        Scope scope = Props.getScope();
        String path = FileUtil.pathFromParts(PlyUtil.SYSTEM_CONFIG_DIR.getPath(), "depmngr" + scope.getFileSuffix() + ".properties");
        PropFile systemDepmngrProps = PropFiles.load(path, false, false);
        if (!systemDepmngrProps.contains("localRepo")) {
            return null;
        } else {
            return systemDepmngrProps.get("localRepo").value();
        }
    }

    /**
     * The intellij-home defined at {@see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html}
     * is not uniquely defined as it references {@literal <product name>} but gives no help in determining what
     * that value is.  This document, {@see http://devnet.jetbrains.net/docs/DOC-181}, says that it should be of the
     * form {@literal .IntelliJIdeaXX} where XX is the product version.  However on my local box, it is of the form
     * {@literal .IdeaICXX} where, again, XX is the product version.  I have the community-edition so perhaps that
     * is the cause of discrepancy.  Additionally, on a {@literal MacOSX} box, it is of the format {@literal IdeaICXX}
     * for the community-edition and {@literal IntelliJIdeaXX} for the full product (notice the lack of dot prefix).
     * This method will assume all are valid and check for all aforementioned patterns.
     * @return the INTELLIJ_HOME directory or null if it cannot be found
     * @see {@literal http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html}
     * @see {@literal http://devnet.jetbrains.net/docs/DOC-181}
     */
    private static File getIntellijDirectory() {
        File userHomeBasePath;
        String userHome = System.getProperty("user.home");
        // @see http://www.jetbrains.com/idea/webhelp/project-and-ide-settings.html
        if (JnaUtil.getOperatingSystem() == JnaUtil.Os.OSX) {
            userHomeBasePath = FileUtil.fromParts(userHome, "Library", "Preferences");
        } else {
            userHomeBasePath = new File(userHome);
        }
        if (!userHomeBasePath.exists()) {
            return null;
        }
        // now find the intellij-directory; either '.IntellijIdea*' or '.IdeaIC*'
        File[] intellijDirectories = userHomeBasePath.listFiles(new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return (name.startsWith(".IntelliJIdea") || name.startsWith(".IdeaIC")
                            || name.startsWith("IntelliJIdea") || name.startsWith("IdeaIC"));
            }
        });
        if ((intellijDirectories == null) || (intellijDirectories.length == 0)) {
            return null;
        }
        // if more than one, well, if we're !headless let's just ask, otherwise just return the first
        if ((intellijDirectories.length > 1) && !PlyUtil.isHeadless()) {
            return getChosenDirectory(intellijDirectories);
        } else {
            return intellijDirectories[0];
        }
    }

    /**
     * Asks the user which of {@code intellijDirectories} to use.
     * @param intellijDirectories the array of matching intellij directories for which to ask the user which to use
     * @return the user's chosen directory from {@code intellijDirectories} or null if the user aborted or an exception
     *         happened.
     */
    private static File getChosenDirectory(File[] intellijDirectories) {
        String options = "[num|abort]";
        Output.print("^ply^ Found multiple Intellij config directories:");
        int choice = 1;
        for (File intellijDirectory : intellijDirectories) {
            Output.print("^ply^ [^b^%d^r^] %s", choice++, intellijDirectory.getPath());
        }
        // need to go directly to stdout to avoid Output parsing prior to Exec handling
        System.out.println(String.format("^no_line^^ply^ use configuration from %s? ", options));
        while (true) {
            try {
                CharBuffer buffer = CharBuffer.allocate(Integer.valueOf(intellijDirectories.length).toString().length() + 1);
                new InputStreamReader(System.in).read(buffer);
                buffer.rewind();
                String answer = buffer.toString().trim();
                Integer answerAsNumber = null;
                try {
                    answerAsNumber = Integer.parseInt(answer);
                } catch (NumberFormatException nfe) {
                    answerAsNumber = null;
                }
                if (answerAsNumber != null) {
                    int index = answerAsNumber - 1;
                    if ((index >= 0) && (index < intellijDirectories.length)) {
                        return intellijDirectories[index];
                    }
                    System.out.println(String.format("^no_line^^ply^ ^red^invalid number^r^; must be between %d and %d, parse configuration %s ", 1, intellijDirectories.length, options));
                } else if (!"abort".equals(answer)) {
                    System.out.println(String.format("^no_line^^ply^ ^red^invalid option^r^, parse configuration %s ", options));
                } else {
                    return null;
                }
            } catch (IOException ioe) {
                Output.print(ioe);
                return null;
            }
        }
    }

    /**
     * Calls {@link #collectDependencies(File, Scope)} with the value of {@literal ply.scope}
     * @param projectConfigDir @see {@link #collectDependencies(File, Scope)}
     * @return @see {@link #collectDependencies(File, Scope)}
     */
    public static Set<DependencyAtom> collectDependencies(File projectConfigDir) {
        return collectDependencies(projectConfigDir, Props.getScope());
    }

    /**
     * Collects all dependencies for {@code projectDir} by parsing the property names of the
     * {@literal resolved-deps.properties} within the {@literal project.build.dir}.
     * @param projectConfigDir the project configuration directory from which to load properties necessary to
     * @param scope the scope for which to load the {@literal resolved-deps.properties} file.
     * @return the resolved {@link DependencyAtom} objects.
     */
    public static Set<DependencyAtom> collectDependencies(File projectConfigDir, Scope scope) {
        PropFile properties = Deps.getResolvedProperties(projectConfigDir, scope, false);
        Set<DependencyAtom> dependencyAtoms = new HashSet<DependencyAtom>(properties.size());
        for (Prop dependencyAtom : properties.props()) {
            DependencyAtom parsed = DependencyAtom.parse(dependencyAtom.name, null);
            if (parsed != null) {
                dependencyAtoms.add(parsed);
            } else {
                Output.print("^warn^ Could not parse dependency ^b^%s^r^.", dependencyAtom.name);
            }
        }
        return dependencyAtoms;
    }

    /**
     * Reads and parses {@code xmlResource} into a {@link Document} object.  If it can be determined that {@code xmlResource}
     * does not exist, then {@code altXmlResource} will be used.
     * @param xmlResource to parse into a {@link Document}.
     * @param altXmlResource an alternative resource to use (the default template if {@code xmlResource} does not exist)
     * @return a {@link Document} object for the given resource
     * @throws SystemExit upon {@link IOException} or {@link SAXException}
     */
    public static Document readXmlDocument(Resource xmlResource, Resource altXmlResource) throws SystemExit {
        Resource resource = xmlResource;
        if ((xmlResource == null) || xmlResource.getOntology() == Resource.Ontology.DoesNotExist) {
            resource = altXmlResource;
        }
        if (resource == null) {
            return null;
        }
        InputStream stream = null;
        try {
            stream = resource.open();
        } catch (IOException ioe) {
            if (resource == xmlResource) {
                try {
                    stream = altXmlResource.open();
                } catch (IOException altIoe) {
                    Output.print(altIoe);
                    throw new SystemExit(1);
                }
            }
        }
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        } catch (ParserConfigurationException pce) {
            Output.print(pce);
            throw new SystemExit(1);
        } catch (SAXException saxe) {
            Output.print(saxe);
            throw new SystemExit(1);
        } catch (IOException ioe) {
            Output.print(ioe);
            throw new SystemExit(1);
        }
    }

    /**
     * Writes {@code document} out to disk at {@code file}.  If {@code file} does not exist, it will be created.
     * @param file to which to write the content of {@code document}.
     * @param document to write to {@code file}
     * @throws SystemExit upon {@link IOException} or {@link TransformerException}
     */
    public static void writeXmlDocument(File file, Document document) throws SystemExit {
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            Source source = new DOMSource(document);
            Result result = new StreamResult(file);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);
        } catch (TransformerConfigurationException tce) {
            Output.print(tce);
            throw new SystemExit(1);
        } catch (TransformerException te) {
            Output.print(te);
            throw new SystemExit(1);
        } catch (IOException ioe) {
            Output.print(ioe);
            throw new SystemExit(1);
        }
    }

    /**
     * If there is no such {@link Element} object one will be created on {@code element}.
     * @param element the root element from which to start searching
     * @param attributeValue attribute value of the {@literal name} attribute on the {@literal <component>}
     *                       {@link Element} object which to find
     * @return an a {@literal <component>} {@link Element} object with attribute named {@code attributeName}
     */
    public static Element findComponent(Element element, String attributeValue) {
        return findElement(element, "component", attributeValue);
    }

    /**
     * If there is no such element, one will be created on {@code element}.
     * @param element the root element from which to start searching
     * @param elementName element name of an {@link Element} object which to find
     * @param attributeValue attribute value of the {@literal name} attribute on the {@code elementName} tag which to find
     * @return an {@link Element} object named {@code elementName} which has an attribute named {@code attributeValue}
     */
    public static Element findElement(Element element, String elementName, String attributeValue) {
        NodeList children = element.getElementsByTagName(elementName);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (!(child instanceof Element)) {
                continue;
            }
            Element childElement = (Element) child;
            if (childElement.hasAttribute("name") && attributeValue.equals(childElement.getAttribute("name"))) {
                return childElement;
            }
        }
        Element createdElement = createElement(element, elementName);
        createdElement.setAttribute("name", attributeValue);
        return createdElement;
    }

    /**
     * If there is no such element, one will be created on {@code element}.
     * @param element the root element from which to start searching
     * @param elementName element name of an {@link Element} object which to find
     * @return an {@link Element} object named {@code elementName} on {@code element}
     */
    public static Element findElement(Element element, String elementName) {
        NodeList elements = element.getElementsByTagName(elementName);
        if ((elements == null) || (elements.getLength() == 0)) {
            return createElement(element, elementName);
        }
        // return first if more than one...TODO - is that ok?
        return (Element) elements.item(0);
    }

    protected static Element createElement(Element element, String name) {
        Element child = element.getOwnerDocument().createElement(name);
        element.appendChild(child);
        return child;
    }

    /**
     * @param element the element to check
     * @param elementName the name of the child-element to check for on {@code element}
     * @return true if {@code element} has at least one child element named {@code elementName}
     */
    public static boolean hasElement(Element element, String elementName) {
        NodeList elements = element.getElementsByTagName(elementName);
        return ((elements != null) && (elements.getLength() != 0));
    }

    /**
     * Removes all children named {@code named} from element {@code from}
     * @param from which to remove items named {@code named}
     * @param named is the name of the elements to remove from {@code from}
     */
    public static void removeElements(Element from, String named) {
        NodeList children = from.getChildNodes();
        boolean removeNextNewline = false;
        List<Node> removes = new ArrayList<Node>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ((child != null) && named.equals(child.getNodeName())) {
                removes.add(child);
                removeNextNewline = true;
            } else if (removeNextNewline && (child != null) && (child instanceof Text)
                    && child.getNodeValue().trim().isEmpty()) {
                removes.add(child);
            } else {
                removeNextNewline = false;
            }
        }
        for (Node node : removes) {
            from.removeChild(node);
        }
    }

    /**
     * Sets the value of the jdk to an attribute named {@code attributeName} on {@code component}
     * @param component on which to set an attributed named {@code attributeName} with the jdk version
     * @param attributeName name of the attribute
     */
    public static void setLanguageAttribute(Element component, String attributeName) {
        String languageLevel = Props.get("languageLevel", Context.named("intellij")).value();
        if (languageLevel.isEmpty()) {
            languageLevel = "JDK_" + getJavaVersion().replace(".", "_");
        }
        component.setAttribute(attributeName, languageLevel);
    }

    public static String getJavaVersion() {
        String version = System.getProperty("java.version");
        // only take the first decimal if multiple
        if ((version.length() > 2) && (version.charAt(1) == '.')) {
            version = version.substring(0, 3);
        }
        return version;
    }

    public static Scope getSubmodulesScope() {
        // get project.submodules.scope prop from props to see if scope for submodules has changed
        // note, getting props not from parentConfig as the definition happens via the parent's ad-hoc
        // prop (i.e., -Pproject.submodules.scope=intellij)
        Prop submodulesScopeProp = Props.get("submodules.scope", Context.named("project"), Props.getScope());
        return (Prop.Empty.equals(submodulesScopeProp) ? Props.getScope() : Scope.named(submodulesScopeProp.value()));
    }

}
