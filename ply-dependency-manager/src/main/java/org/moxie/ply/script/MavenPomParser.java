package org.moxie.ply.script;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 9/30/11
 * Time: 4:18 PM
 *
 * Responsible for parsing {@literal Maven} pom files, resolving all dependencies including the dependencies'
 * {@literal groupId}, {@literal artifactId} and {@literal version} from any property values referenceable from
 * the pom file.  Parsing a given pom will resolve parent pom information if present.
 */
public interface MavenPomParser {

    static class Default implements  MavenPomParser {

        /**
         * Holds a collection of resolved property values and un-resolved dependencies from parsing a pom file.
         */
        private static class ParseResult {
            private final Map<String, String> mavenProperties;
            private final Map<String, String> mavenDependencies;
            public ParseResult() {
                this.mavenProperties = new HashMap<String, String>();
                this.mavenDependencies = new HashMap<String, String>();
            }
        }

        @Override public Properties parsePom(String pomUrlPath, DependencyManager.RepositoryAtom repositoryAtom) {
            try {
                ParseResult result = parse(pomUrlPath, repositoryAtom);
                Properties properties = new Properties();
                for (String dependencyKey : result.mavenDependencies.keySet()) {
                    String filteredDependencyKey = dependencyKey;
                    String filteredDependencyValue = result.mavenDependencies.get(dependencyKey);
                    if (filteredDependencyKey.contains("${") || filteredDependencyValue.contains("${")) {
                        for (String mavenProperty : result.mavenProperties.keySet()) {
                            if (filteredDependencyKey.contains("${" + mavenProperty + "}")) {
                                filteredDependencyKey = filteredDependencyKey.replaceAll("\\$\\{" + mavenProperty.replaceAll("\\.", "\\\\.") + "\\}",
                                        result.mavenProperties.get(mavenProperty));
                            }
                            if (filteredDependencyValue.contains("${" + mavenProperty + "}")) {
                                filteredDependencyValue = filteredDependencyValue.replaceAll("\\$\\{" + mavenProperty.replaceAll("\\.", "\\\\.") + "\\}",
                                        result.mavenProperties.get(mavenProperty));
                            }
                        }
                    }
                    properties.put(filteredDependencyKey, filteredDependencyValue);
                }
                return properties;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        public ParseResult parse(String pomUrlPath, DependencyManager.RepositoryAtom repositoryAtom)
                throws ParserConfigurationException, IOException, SAXException {
            ParseResult parseResult = new ParseResult();
            parse(pomUrlPath, repositoryAtom, parseResult);
            return parseResult;
        }

        private void parse(String pomUrlPath, DependencyManager.RepositoryAtom repositoryAtom, ParseResult parseResult)
                throws ParserConfigurationException, IOException, SAXException {
            URL pomUrl = new URL(pomUrlPath);
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pomUrl.openStream());
            NodeList pomChildren = document.getDocumentElement().getChildNodes();
            // store the parent pom url so that recursive processing is down after the entire current pom is analyzed
            // so that any local property filtering (i.e., version) can be done.
            String parentPomUrlPath = null;
            // store the parent version in case the version of the project is not explicitly specified, will use
            // parent's per maven convention.
            AtomicReference<String> parentVersion = new AtomicReference<String>("");
            String localVersion = null;
            for (int i = 0; i < pomChildren.getLength(); i++) {
                Node child = pomChildren.item(i);
                String nodeName = child.getNodeName();
                if (!"dependencies".equals(nodeName)
                        && !"properties".equals(nodeName)
                        && !"parent".equals(nodeName)
                        && !"version".equals(nodeName)) {
                    continue;
                }
                if ("dependencies".equals(nodeName)) {
                    parseDependencies(child, parseResult);
                } else if ("properties".equals(nodeName)) {
                    parseProperties(child, parseResult);
                } else if ("version".equals(nodeName)) {
                    localVersion = child.getTextContent();
                } else { // parent
                    parentPomUrlPath = parseParentPomUrlPath(child, repositoryAtom, parentVersion);
                }
            }
            parseResult.mavenProperties.put("project.version", (localVersion != null ? localVersion : parentVersion.get()));
            if (parentPomUrlPath != null) {
                // filter project.version so that it is not overridden by the recursion on parent
                filterVersion(parseResult);
                parse(parentPomUrlPath, repositoryAtom, parseResult);
            }
        }

        private void parseDependencies(Node dependenciesNode, ParseResult parseResult) {
            NodeList dependencies = dependenciesNode.getChildNodes();
            depLoop : for (int i = 0; i < dependencies.getLength(); i++) {
                if (!"dependency".equals(dependencies.item(i).getNodeName())) {
                    continue;
                }
                NodeList dependencyNode = dependencies.item(i).getChildNodes();
                String groupId = "", artifactId = "", version = "", classifier = "", type = "";
                for (int j = 0; j < dependencyNode.getLength(); j++) {
                    Node child = dependencyNode.item(j);
                    if ("groupId".equals(child.getNodeName())) {
                        groupId = child.getTextContent();
                    } else if ("artifactId".equals(child.getNodeName())) {
                        artifactId = child.getTextContent();
                    } else if ("version".equals(child.getNodeName())) {
                        version = child.getTextContent();
                    } else if ("classifier".equals(child.getNodeName())) {
                        classifier = child.getTextContent();
                    } else if ("type".equals(child.getNodeName())) {
                        type = child.getTextContent();
                    } else if ("scope".equals(child.getNodeName())) {
                        if (!"compile".equals(child.getTextContent())) {
                            // TODO - revisit ... only include compile scoped deps as rest are not for compilation
                            // TODO - and are not transitive; see http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html.
                            continue depLoop;
                        }
                    } else if ("optional".equals(child.getNodeName())) {
                        if (Boolean.valueOf(child.getTextContent())) {
                            continue depLoop;
                        }
                    } else if ("systemPath".equals(child.getNodeName())) {
                        // always skip these, no way to resolve them
                        continue depLoop;
                    }
                }
                // iterating child->parent, per maven, child overrides parent, only place in if not already exists.
                if (!parseResult.mavenDependencies.containsKey(groupId + "::" + artifactId)) {
                    if (classifier.isEmpty() && (type.isEmpty() || "jar".equals(type))) {
                        parseResult.mavenDependencies.put(groupId + "::" + artifactId, version);
                    } else {
                        String artifactName = artifactId + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + (type.isEmpty() ? "jar" : type);
                        parseResult.mavenDependencies.put(groupId + "::" + artifactId, version + "::" + artifactName);
                    }
                }
            }
        }

        // each reference to ${project.version} needs to be resolved prior to recurring on parent as it is relative
        // to the current pom.  so, as opposed to the other properties which are unique across pom-hierarchy (TODO sans
        // other project information), need to filter before each recursion.
        private void filterVersion(ParseResult parseResult) {
            if (!parseResult.mavenProperties.containsKey("project.version")) {
                return;
            }
            Map<String, String> filteredDeps = new HashMap<String, String>(parseResult.mavenDependencies.size());
            for (String dependencyKey : parseResult.mavenDependencies.keySet()) {
                String filteredDependencyKey = dependencyKey;
                String filteredDependencyValue = parseResult.mavenDependencies.get(dependencyKey);
                if (filteredDependencyKey.contains("${project.version}")) {
                    filteredDependencyKey = filteredDependencyKey.replaceAll("\\$\\{project\\.version\\}",
                            parseResult.mavenProperties.get("project.version"));
                }
                if (filteredDependencyValue.contains("${project.version}")) {
                    filteredDependencyValue = filteredDependencyValue.replaceAll("\\$\\{project\\.version\\}",
                            parseResult.mavenProperties.get("project.version"));
                }
                filteredDeps.put(filteredDependencyKey, filteredDependencyValue);
            }
            parseResult.mavenDependencies.clear();
            parseResult.mavenDependencies.putAll(filteredDeps);
        }

        private void parseProperties(Node propertiesNode, ParseResult parseResult) {
            NodeList properties = propertiesNode.getChildNodes();
            for (int i = 0; i < properties.getLength(); i++) {
                Node child = properties.item(i);
                // iterating child->parent, per maven, child overrides parent, only place in if not already exists.
                if (!parseResult.mavenProperties.containsKey(child.getNodeName())) {
                    parseResult.mavenProperties.put(child.getNodeName(), child.getTextContent());
                }
            }
        }

        private String parseParentPomUrlPath(Node parent, DependencyManager.RepositoryAtom repositoryAtom,
                                             AtomicReference<String> parentVersion) {
            NodeList children = parent.getChildNodes();
            String groupId = "", artifactId = "", version = "";
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if ("groupId".equals(child.getNodeName())) {
                    groupId = child.getTextContent();
                } else if ("artifactId".equals(child.getNodeName())) {
                    artifactId = child.getTextContent();
                } else if ("version".equals(child.getNodeName())) {
                    version = child.getTextContent();
                    parentVersion.set(version);
                }
            }

            String startPath = repositoryAtom.getPropertyName();
            // hygiene the end separator
            if (!startPath.endsWith("/") && !startPath.endsWith("\\")) {
                startPath = startPath + File.separator;
            }
            String endPath = groupId.replaceAll("\\.", File.separator)
                    + File.separator + artifactId + File.separator +
                    version + File.separator + (artifactId + "-" + version) + ".pom";
            // hygiene the start separator
            if (endPath.startsWith("/") || endPath.startsWith("\\")) {
                endPath = endPath.substring(1, endPath.length());
            }
            return startPath + endPath;
        }

    }

    Properties parsePom(String pomUrlPath, DependencyManager.RepositoryAtom repositoryAtom);

}
