package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.input.Resource;
import net.ocheyedan.ply.input.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/1/11
 * Time: 2:14 PM
 *
 * Responsible for parsing {@literal Maven} pom files, resolving all dependencies including the dependencies'
 * {@literal groupId}, {@literal artifactId} and {@literal version} from any property values reference-able from
 * the pom file.  Parsing a given pom will resolve parent pom information if present.
 */
public interface MavenPomParser {

    /**
     * Default implementation of {@link MavenPomParser}.  This parser is rudimentary and is definitely a work
     * in progress.
     */
    static class Default implements MavenPomParser {

        /**
         * Holds a collection of resolved property values and un-resolved dependencies and repositories from
         * parsing a pom file.
         */
        private static class ParseResult {

            /**
             * An interim dependency; may not have all information until things like <dependencyManagement> tags are parsed.
             */
            private static class Incomplete {
                private String groupId;
                private String artifactId;
                private String version;
                private String classifier;
                private String type;
                private String scope;
                private String optional;
                private Boolean systemPath;
                private boolean resolutionOnly; // for deps found in dep-mngt (being kept for version resolution)
                private Incomplete(String groupId, String artifactId, String version, String classifier, String type,
                                   String scope, String optional, Boolean systemPath, boolean resolutionOnly) {
                    this.groupId = groupId;
                    this.artifactId = artifactId;
                    this.version = version;
                    this.classifier = classifier;
                    this.type = type;
                    this.scope = scope;
                    this.optional = optional;
                    this.systemPath = systemPath;
                    this.resolutionOnly = resolutionOnly;
                }
                private void complete(Map<String, Map<String, String>> placement) {
                    if (resolutionOnly || shouldSkip(scope, systemPath)) {
                        return; // not applicable
                    } else if ((version == null) || version.isEmpty()) {
                        Output.print("^warn^ Encountered dependency without a version - %s:%s:%s:%s:%s", groupId, artifactId, version, classifier, type);
                    }
                    boolean transientDep = ("provided".equals(scope) || Boolean.valueOf(optional));
                    DependencyAtom atom;
                    if (classifier.isEmpty() && (type.isEmpty() || "jar".equals(type))) {
                        atom = new DependencyAtom(groupId, artifactId, version, transientDep);
                    } else {
                        String artifactName = artifactId + "-" + version + (classifier.isEmpty() ? "" : "-" + classifier) + "." + (type.isEmpty() ? "jar" : type);
                        atom = new DependencyAtom(groupId, artifactId, version, artifactName, transientDep);
                    }
                    String plyScope = ("test".equals(scope) ? "test" : "");
                    Map<String, String> scopedPlacement = placement.get(plyScope);
                    if (scopedPlacement == null) {
                        scopedPlacement = new HashMap<String, String>();
                        placement.put(plyScope, scopedPlacement);
                    }
                    scopedPlacement.put(atom.getPropertyName(), atom.getPropertyValue());
                }
            }

            private final Map<String, String> mavenProperties;

            private final Map<String, Incomplete> mavenIncompleteDeps;

            private final Set<String> mavenRepositoryUrls;

            private final Set<String> modules;

            private ParseResult() {
                this.mavenProperties = new HashMap<String, String>();
                this.mavenIncompleteDeps = new HashMap<String, Incomplete>();
                this.mavenRepositoryUrls = new HashSet<String>(2);
                this.modules = new HashSet<String>(4);
            }

            private void addDep(String groupId, String artifactId, String version, String classifier, String type,
                                String scope, String optional, Boolean systemPath, boolean overrideExisting, boolean resolutionOnly) {
                // @see Note here http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management
                // for description of key
                String key = groupId + ":" + artifactId + ":" + (type == null ? "" : type) + ":" + (classifier == null ? "" : classifier);
                if (mavenIncompleteDeps.containsKey(key)) {
                    Incomplete incomplete = mavenIncompleteDeps.get(key);
                    incomplete.version = (overrideExisting
                            || ((incomplete.version == null) || incomplete.version.isEmpty())) ? version : incomplete.version;
                    incomplete.classifier = (overrideExisting
                            || ((incomplete.classifier == null) || incomplete.classifier.isEmpty())) ? classifier : incomplete.classifier;
                    incomplete.type = (overrideExisting
                            || ((incomplete.type == null) || incomplete.type.isEmpty())) ? type : incomplete.type;
                    incomplete.scope = (overrideExisting
                            || ((incomplete.scope == null) || incomplete.scope.isEmpty()) ? scope : incomplete.scope);
                    incomplete.optional = (overrideExisting
                            || ((incomplete.optional == null) || incomplete.optional.isEmpty()) ? optional : incomplete.optional);
                    incomplete.systemPath = (overrideExisting || incomplete.systemPath == null ? systemPath : incomplete.systemPath);
                    // not applicable for overrides...must set regardless
                    if (incomplete.resolutionOnly && !resolutionOnly) {
                        incomplete.resolutionOnly = false;
                    }
                } else {
                    mavenIncompleteDeps.put(key, new Incomplete(groupId, artifactId, version, classifier, type, scope,
                            optional, systemPath, resolutionOnly));
                }
            }

            private void addRepo(String repoUrl) {
                this.mavenRepositoryUrls.add(repoUrl);
            }

            private static boolean shouldSkip(String scope, Boolean systemPath) {
                if ((systemPath != null) && systemPath) {
                    return true;
                }
                // compile/runtime will become deps in ply and provided will become transient deps in ply
                else if ((scope != null) && !scope.isEmpty() && "system".equals(scope)) {
                    return true;
                }
                // optional will become transient deps in ply

                return false;
            }

            private Map<String, Map<String, String>> resolveDeps() {
                Map<String, Map<String, String>> deps = new HashMap<String, Map<String, String>>(mavenIncompleteDeps.size());
                for (Incomplete incomplete : mavenIncompleteDeps.values()) {
                    incomplete.complete(deps);
                }
                return deps;
            }
        }

        @Override public MavenPom parsePom(String pomUrlPath, RepositoryAtom repositoryAtom) {
            try {
                ParseResult result = parse(pomUrlPath, repositoryAtom);
                Properties deps = new Properties();
                Properties testDeps = new Properties();
                Map<String, Map<String, String>> resolvedDeps = result.resolveDeps();
                for (String scope : resolvedDeps.keySet()) {
                    Map<String, String> scopedResolvedDeps = resolvedDeps.get(scope);
                    Properties scopedDeps = ("test".equals(scope) ? testDeps : deps); // maven for ply has either test or default scoped deps.
                    for (String dependencyKey : scopedResolvedDeps.keySet()) {
                        String filteredDependencyKey = dependencyKey;
                        String filteredDependencyValue = scopedResolvedDeps.get(dependencyKey);
                        if (filteredDependencyKey.contains("${") || filteredDependencyValue.contains("${")) {
                            for (String mavenProperty : result.mavenProperties.keySet()) {
                                filteredDependencyKey = filter(filteredDependencyKey, mavenProperty, result.mavenProperties);
                                filteredDependencyValue = filter(filteredDependencyValue, mavenProperty, result.mavenProperties);
                            }
                        }
                        scopedDeps.put(filteredDependencyKey, filteredDependencyValue);
                    }
                }
                Properties repos = new Properties();
                for (String repoUrl : result.mavenRepositoryUrls) {
                    RepositoryAtom repoAtom = RepositoryAtom.parse("maven:" + repoUrl);
                    repos.put(repoAtom.getPropertyName(), repoAtom.getPropertyValue());
                }
                Properties modules = new Properties();
                for (String module : result.modules) {
                    modules.put(module, "");
                }
                return new MavenPom(result.mavenProperties.get("project.groupId"),
                        result.mavenProperties.get("project.artifactId"),
                        result.mavenProperties.get("project.version"),
                        result.mavenProperties.get("project.packaging"),
                        deps, testDeps, repos, modules,
                        result.mavenProperties.get("project.build.directory"),
                        result.mavenProperties.get("project.build.outputDirectory"),
                        result.mavenProperties.get("project.build.finalName"),
                        result.mavenProperties.get("project.build.sourceDirectory"),
                        result.mavenProperties.get("project.build.testOutputDirectory"),
                        result.mavenProperties.get("project.build.testSourceDirectory"));
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }

        public ParseResult parse(String pomUrlPath, RepositoryAtom repositoryAtom)
                throws ParserConfigurationException, IOException, SAXException {
            ParseResult parseResult = new ParseResult();
            parse(pomUrlPath, repositoryAtom, parseResult);
            return parseResult;
        }

        private void parse(String pomUrlPath, RepositoryAtom repositoryAtom, ParseResult parseResult)
                throws ParserConfigurationException, IOException, SAXException {
            Resource pomResource = Resources.parse(pomUrlPath);
            try {
                InputStream stream = pomResource.open();
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
                NodeList pomChildren = document.getDocumentElement().getChildNodes();
                // store the parent pom url so that recursive processing is down after the entire current pom is analyzed
                // so that any local property filtering (i.e., version) can be done.
                String parentPomUrlPath = null;
                // store the parent version in case the version of the project is not explicitly specified, will use
                // parent's per maven convention.
                AtomicReference<String> parentVersion = new AtomicReference<String>("");
                String localVersion = null;
                // similar to the parent version, need to store the parent's groupId
                AtomicReference<String> parentGroupId = new AtomicReference<String>("");
                String localGroupId = null, localArtifactId = null, packaging = null;
                for (int i = 0; i < pomChildren.getLength(); i++) {
                    Node child = pomChildren.item(i);
                    String nodeName = child.getNodeName();
                    if ("dependencyManagement".equals(nodeName)) {
                        parseDependencyManagement(child, parseResult, repositoryAtom);
                    } else if ("dependencies".equals(nodeName)) {
                        parseDependencies(child, parseResult, repositoryAtom, false);
                    } else if ("repositories".equals(nodeName)) {
                        parseRepositories(child, parseResult);
                    } else if ("properties".equals(nodeName)) {
                        parseProperties(child, parseResult);
                    } else if ("groupId".equals(nodeName) && !"${parent.groupId}".equals(child.getTextContent())) {
                        localGroupId = child.getTextContent();
                    } else if ("artifactId".equals(nodeName)) {
                        localArtifactId = child.getTextContent();
                    } else if ("version".equals(nodeName) && !"${parent.version}".equals(child.getTextContent())) {
                        localVersion = Version.resolve(child.getTextContent(), getMetadataBaseUrl(pomUrlPath));
                    } else if ("packaging".equals(nodeName)) {
                        packaging = child.getTextContent();
                    } else if ("parent".equals(nodeName)) { // parent
                        parentPomUrlPath = parseParentPomUrlPath(child, repositoryAtom, parentGroupId, parentVersion);
                    } else if ("build".equals(nodeName)) {
                        parseBuild(child, parseResult);
                    } else if ("modules".equals(nodeName)) {
                        parseModules(child, parseResult);
                    }
                }
                if (!parseResult.mavenProperties.containsKey("project.groupId")) {
                    parseResult.mavenProperties.put("project.groupId", (localGroupId != null ? localGroupId : parentGroupId.get()));
                }
                if (!parseResult.mavenProperties.containsKey("project.artifactId")) { // don't override artifactId with parent.artifactId
                    parseResult.mavenProperties.put("project.artifactId", localArtifactId);
                }
                if (!parseResult.mavenProperties.containsKey("project.version")) {
                    parseResult.mavenProperties.put("project.version", (localVersion != null ? localVersion : parentVersion.get()));
                }
                if (!parseResult.mavenProperties.containsKey("project.packaging")) {
                    parseResult.mavenProperties.put("project.packaging", packaging);
                }
                if (parentPomUrlPath != null) {
                    // filter project.* so that they are not overridden by the recursion on parent
                    filterLocalProjectProperties(parseResult);
                    parse(parentPomUrlPath, repositoryAtom, parseResult);
                }
            } finally {
                pomResource.close();
            }
        }

        private String getMetadataBaseUrl(String pomUrlPath) {
            int index = pomUrlPath.lastIndexOf("/");
            if (index == -1) {
                return null;
            }
            String url = pomUrlPath.substring(0, index);
            index = url.lastIndexOf("/");
            if (index == -1) {
                return null;
            }
            return url.substring(0, index);
        }

        private String getMetadataBaseUrl(RepositoryAtom repositoryAtom, String groupId, String artifactId) {
            String repoUrl = repositoryAtom.getPropertyName();
            return (repoUrl + (repoUrl.endsWith("/") ? "" : "/") + groupId.replaceAll("\\.", "/") + "/" + artifactId);
        }

        private void parseDependencyManagement(Node dependencyManagementNode, ParseResult parseResult, RepositoryAtom repositoryAtom) {
            NodeList dependencyManagementChildren = dependencyManagementNode.getChildNodes();
            for (int i = 0; i < dependencyManagementChildren.getLength(); i++) {
                Node dependenciesNode = dependencyManagementChildren.item(i);
                if ("dependencies".equals(dependenciesNode.getNodeName())) {
                    parseDependencies(dependenciesNode, parseResult, repositoryAtom, true);
                    break;
                }
            }
        }

        private void parseDependencies(Node dependenciesNode, ParseResult parseResult, RepositoryAtom repositoryAtom, boolean resolutionOnly) {
            NodeList dependencies = dependenciesNode.getChildNodes();
            for (int i = 0; i < dependencies.getLength(); i++) {
                if (!"dependency".equals(dependencies.item(i).getNodeName())) {
                    continue;
                }
                NodeList dependencyNode = dependencies.item(i).getChildNodes();
                String groupId = "", artifactId = "", version = "", classifier = "", type = "", scope = "", optional = "";
                Boolean systemPath = null;
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
                        scope = child.getTextContent();
                    } else if ("optional".equals(child.getNodeName())) {
                        optional = child.getTextContent();
                    } else if ("systemPath".equals(child.getNodeName())) {
                        systemPath = true;
                    }
                }
                version = Version.resolve(version, getMetadataBaseUrl(repositoryAtom, groupId, artifactId));
                // iterating child->parent, per maven, child overrides parent, only place in if not already
                // exists (hence !override).
                parseResult.addDep(groupId, artifactId, version, classifier, type, scope, optional, systemPath, false, resolutionOnly);
            }
        }

        /**
         * Parses the {@literal <repositories>} tag and extracts all default layout repository urls and places them
         * within {@code parseResult}
         * @param repositoriesNode the {@literal <repositories>} tag
         * @param parseResult into which to place parsed repository urls.
         */
        private void parseRepositories(Node repositoriesNode, ParseResult parseResult) {
            NodeList repositories = repositoriesNode.getChildNodes();
            for (int i = 0; i < repositories.getLength(); i++) {
                if (!"repository".equals(repositories.item(i).getNodeName())) {
                    continue;
                }
                NodeList repositoryNode = repositories.item(i).getChildNodes();
                String repoUrl = "", layout = "";
                for (int j = 0; j < repositoryNode.getLength(); j++) {
                    Node child = repositoryNode.item(j);
                    if ("url".equals(child.getNodeName())) {
                        repoUrl = child.getTextContent();
                    } else if ("layout".equals(child.getNodeName())) {
                        layout = child.getTextContent();
                    }
                }
                if (!repoUrl.isEmpty() && (layout.isEmpty() || "default".equals(layout))) {
                    parseResult.addRepo(repoUrl);
                } else if (!layout.isEmpty()) {
                    Output.print("^warn^ Found a repository [ %s ] however its layout [ ^b^%s^r^ ] is not supported, skipping.", repoUrl, layout);
                }
            }
        }

        private void parseBuild(Node buildNode, ParseResult parseResult) {
            NodeList build = buildNode.getChildNodes();
            for (int i = 0; i < build.getLength(); i++) {
                Node child = build.item(i);
                String nodeName = child.getNodeName();
                // TODO - all the following need to be filtered.
                if ("directory".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.directory", child.getTextContent());
                } else if ("outputDirectory".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.outputDirectory", child.getTextContent());
                } else if ("sourceDirectory".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.sourceDirectory", child.getTextContent());
                } else if ("testOutputDirectory".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.testOutputDirectory", child.getTextContent());
                } else if ("testSourceDirectory".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.testSourceDirectory", child.getTextContent());
                } else if ("finalName".equals(nodeName)) {
                    parseResult.mavenProperties.put("project.build.finalName", child.getTextContent());
                }
                // TODO - resources / testResources [ requires ply to support multiple-resource dirs ]
            }
        }

        private void parseModules(Node modulesNode, ParseResult parseResult) {
            NodeList modules = modulesNode.getChildNodes();
            for (int i = 0; i < modules.getLength(); i++) {
                if (!"module".equals(modules.item(i).getNodeName())) {
                    continue;
                }
                Node moduleNode = modules.item(i);
                parseResult.modules.add(moduleNode.getTextContent());
            }
        }

        /**
         * Each reference to ${project.*} needs to be resolved prior to recurring on parent as it is relative
         * to the current pom.  So, as opposed to the other properties which are unique across pom-hierarchy (TODO sans
         * other project information), need to filter before each recursion.
         * @param parseResult to filter
         */
        private void filterLocalProjectProperties(ParseResult parseResult) {
            if (!parseResult.mavenProperties.containsKey("project.version")
                    && !parseResult.mavenProperties.containsKey("project.groupId")) {
                return;
            }
            Map<String, ParseResult.Incomplete> filteredDeps = new HashMap<String, ParseResult.Incomplete>(parseResult.mavenIncompleteDeps.size());
            for (String dependencyKey : parseResult.mavenIncompleteDeps.keySet()) {
                String filteredDependencyKey = dependencyKey;
                ParseResult.Incomplete incomplete = parseResult.mavenIncompleteDeps.get(dependencyKey);
                filteredDependencyKey = filter(filteredDependencyKey, "project.groupId", parseResult.mavenProperties);
                incomplete.groupId = filter(incomplete.groupId, "project.groupId", parseResult.mavenProperties);
                incomplete.version = filter(incomplete.version, "project.version", parseResult.mavenProperties);
                filteredDeps.put(filteredDependencyKey, incomplete);
            }
            parseResult.mavenIncompleteDeps.clear();
            parseResult.mavenIncompleteDeps.putAll(filteredDeps);
        }

        private static String filter(String toFilter, String filterValue, Map<String, String> replacementMap) {
            if ((toFilter == null) || (filterValue == null)) {
                return toFilter;
            }
            if (toFilter.contains("${" + filterValue + "}")) {
                return toFilter.replaceAll(Pattern.quote("${" + filterValue + "}"), replacementMap.get(filterValue));
            }
            return toFilter;
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

        private String parseParentPomUrlPath(Node parent, RepositoryAtom repositoryAtom,
                                             AtomicReference<String> parentGroupId, AtomicReference<String> parentVersion) {
            NodeList children = parent.getChildNodes();
            String groupId = "", artifactId = "", version = "";
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if ("groupId".equals(child.getNodeName())) {
                    groupId = child.getTextContent();
                    parentGroupId.set(groupId);
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
            String pomUrlPath = startPath + endPath;
            parentVersion.set(Version.resolve(parentVersion.get(), getMetadataBaseUrl(pomUrlPath)));
            return startPath + endPath;
        }

    }

    /**
     * Parses the pom file represented by {@code pomUrlPath} positioned at {@code repositoryAtom}.
     * @param pomUrlPath to parse
     * @param repositoryAtom from which to resolve subsequently found dependencies (TODO - augment as list)
     * @return the parsed pom file as a {@link MavenPom}
     */
    MavenPom parsePom(String pomUrlPath, RepositoryAtom repositoryAtom);

}
