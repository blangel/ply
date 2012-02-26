package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
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
import java.util.concurrent.atomic.AtomicReference;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 12/3/11
 * Time: 2:27 PM
 *
 * Utility to deal with {@literal Intellij} specific files/apis/etc.
 */
public class IntellijUtil {

    private static final AtomicReference<List<String>> cache = new AtomicReference<List<String>>();

    /**
     * The {@literal submodules} property context is first resolved.  If it exists then its values are returned.
     * Otherwise, the comma-delimited list at property {@literal intellij[.scope].submodules} is resolved. If
     * there are no sub-modules then null is returned.
     * @return the modules of this project.
     */
    public static List<String> getModules() {
        if (cache.get() != null) {
            return cache.get();
        }
        PropFileChain submodules = null;
        if (((submodules = Props.get(Context.named("submodules"))) != null) && !submodules.props().iterator().hasNext()) {
            List<String> submoduleNames = new ArrayList<String>();
            int i = 0;
            for (Prop submodule : submodules.props()) {
                submoduleNames.add(submodule.name); // TODO - recursively get submodules' submodules, if any
            }
            cache.set(submoduleNames);
            return submoduleNames;
        }
        return null;
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
        Element plyRepoPathMacroElement = findElement(componentElement, "macro", plyRepoPathMacroName);
        // fourth, set the path macro value (even if already set as the user is choosing to run this script so likely
        // the value needs to be updated).
        String localRepoPath = localRepo.getPropertyName();
        if (localRepoPath.startsWith("file://")) {
            localRepoPath = localRepoPath.substring(7);
        }
        plyRepoPathMacroElement.setAttribute("value", localRepoPath);
        writeXmlDocument(pathMacrosXmlFile, pathMacrosXmlFileDoc);
        return plyRepoPathMacroName;
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
     * is the cause of discrepancy.  This method will assume both are valid and check for both patterns.
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
                return (name.startsWith(".IntellijIdea") || name.startsWith(".IdeaIC"));
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
        Output.printNoLine("^ply^ use configuration from %s? ", options);
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
                    Output.printNoLine("^ply^ ^red^invalid number^r^; must be between %d and %d, parse configuration %s ", 1, intellijDirectories.length, options);
                } else if (!"abort".equals(answer)) {
                    Output.printNoLine("^ply^ ^red^invalid option^r^, parse configuration %s ", options);
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
        Scope scope = Scope.named(Props.get("scope", Context.named("ply")).value());
        return collectDependencies(projectConfigDir, scope);
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
     */
    public static Document readXmlDocument(Resource xmlResource, Resource altXmlResource) {
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
                    System.exit(1);
                }
            }
        }
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
        } catch (ParserConfigurationException pce) {
            Output.print(pce);
            System.exit(1);
        } catch (SAXException saxe) {
            Output.print(saxe);
            System.exit(1);
        } catch (IOException ioe) {
            Output.print(ioe);
            System.exit(1);
        }
        throw new AssertionError("Programming error."); // should never get here
    }

    /**
     * Writes {@code document} out to disk at {@code file}.  If {@code file} does not exist, it will be created.
     * @param file to which to write the content of {@code document}.
     * @param document to write to {@code file}
     */
    public static void writeXmlDocument(File file, Document document) {
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
            System.exit(1);
        } catch (TransformerException te) {
            Output.print(te);
            System.exit(1);
        } catch (IOException ioe) {
            Output.print(ioe);
            System.exit(1);
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

}
