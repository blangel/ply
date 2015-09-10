package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.input.Resource;
import net.ocheyedan.ply.input.Resources;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Scope;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/1/11
 * Time: 2:14 PM
 *
 * Responsible for parsing {@literal Maven} pom files, resolving all dependencies including the dependencies'
 * {@literal groupId}, {@literal artifactId} and {@literal version} from any property values reference-able from
 * the pom file.  Parsing a given pom will resolve parent pom information if present.
 *
 * This parser is rudimentary and is definitely a work in progress
 */
public class MavenPomParser {

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
            private void complete(Map<String, Map<String, String>> placement, ParseResult parseResult) {
                if (resolutionOnly || shouldSkip(scope, systemPath)) {
                    return; // not applicable
                }
                String projectGroupId = parseResult.mavenProperties.get("project.groupId");
                if (((version == null) || version.isEmpty())
                        && ((projectGroupId != null) && projectGroupId.equals(groupId))) {
                    version = parseResult.mavenProperties.get("project.version");
                }
                if ((version == null) || version.isEmpty()) {
                    Output.print("^warn^ Encountered dependency without a version - %s:%s%s", groupId, artifactId,
                            String.format("%s%s", (((classifier != null) && !classifier.isEmpty()) ? ":" + classifier : ""),
                                    (((type != null) && !type.isEmpty()) ? ":" + type : "")));
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

        private String getKey(String groupId, String artifactId, String classifier, String type) {
            // @see Note here http://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management
            // for description of key
            return groupId + ":" + artifactId + ":" + (type == null ? "" : type) + ":" + (classifier == null ? "" : classifier);
        }

        private boolean containsDep(String groupId, String artifactId, String classifier, String type) {
            String key = getKey(groupId, artifactId, classifier, type);
            return mavenIncompleteDeps.containsKey(key);
        }

        private void replaceVersion(String groupId, String artifactId, String classifier, String type, String version) {
            String key = getKey(groupId, artifactId, classifier, type);
            Incomplete incomplete = mavenIncompleteDeps.get(key);
            if (incomplete != null) {
                incomplete.version = version;
            }
        }

        private void addDep(String groupId, String artifactId, String version, String classifier, String type,
                            String scope, String optional, Boolean systemPath, boolean overrideExisting, boolean resolutionOnly) {
            String key = getKey(groupId, artifactId, classifier, type);
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

        private Map<String, Map<String, String>> resolveDeps(MavenPomParser parser, RepositoryAtom repositoryAtom)
                throws ParserConfigurationException, IOException, SAXException {
            resolveImports(parser, repositoryAtom);
            Map<String, Map<String, String>> deps = new HashMap<String, Map<String, String>>(mavenIncompleteDeps.size());
            for (Incomplete incomplete : mavenIncompleteDeps.values()) {
                incomplete.complete(deps, this);
            }
            return deps;
        }

        private void resolveImports(MavenPomParser parser, RepositoryAtom repositoryAtom)
                throws ParserConfigurationException, IOException, SAXException {
            for (Incomplete incomplete : mavenIncompleteDeps.values()) {
                if ("import".equals(incomplete.scope)) {
                    String filteredVersion = filterVersion(incomplete.version, this);
                    Output.print("^dbug^ Dependency has import scope - importing dependencyManagement deps for %s:%s:%s [ pre-filtered version %s ]",
                            incomplete.groupId, incomplete.artifactId, filteredVersion, incomplete.version);
                    PomUri pomUri = parser.createPomUriWithoutRelativePath(repositoryAtom, true, incomplete.groupId, incomplete.artifactId, filteredVersion);
                    ParseResult importParse = new ParseResult();
                    parser.parse(pomUri, repositoryAtom, importParse);
                    parser.parseMavenImport(incomplete, importParse, pomUri.uri, repositoryAtom, this);
                }
            }
        }
    }

    /**
     * Encapsulates the pom url, which may be a relative path (if parsing a parent pom) or an absolute
     * URI.
     */
    private static class PomUri {
        private final String relativeUrl;
        private final String uri;
        private final boolean parsingAsParent; // true if parsing as a parent of another project (i.e., ignore modules)

        private PomUri(String relativeUrl, String uri, boolean parsingAsParent) {
            this.relativeUrl = relativeUrl;
            this.uri = uri;
            this.parsingAsParent = parsingAsParent;
        }
    }

    /**
     * When filtering maven properties, may need to recursively filter. This provides a single object which
     * can be returned from a recursive filter function.
     */
    private static class FilterPair {
        private final String key;
        private final String value;

        public FilterPair(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }


    /**
     * Parses the pom file represented by {@code pomUrlPath} positioned at {@code repositoryAtom}.
     * @param pomUrlPath to parse
     * @param repositoryAtom from which to resolve subsequently found dependencies
     * @return the parsed pom file as a {@link MavenPom}
     */
    public MavenPom parsePom(String pomUrlPath, RepositoryAtom repositoryAtom) {
        try {
            ParseResult result = parse(pomUrlPath, repositoryAtom);
            PropFile deps = new PropFile(Context.named("dependencies"), PropFile.Loc.Local);
            PropFile testDeps = new PropFile(Context.named("dependencies"), Scope.named("test"), PropFile.Loc.Local);
            Map<String, Map<String, String>> resolvedDeps = result.resolveDeps(this, repositoryAtom);
            for (String scope : resolvedDeps.keySet()) {
                Map<String, String> scopedResolvedDeps = resolvedDeps.get(scope);
                PropFile scopedDeps = ("test".equals(scope) ? testDeps : deps); // maven for ply has either test or default scoped deps.
                for (String dependencyKey : scopedResolvedDeps.keySet()) {
                    FilterPair filtered = filterMavenDependency(new FilterPair(dependencyKey, scopedResolvedDeps.get(dependencyKey)), result.mavenProperties);
                    scopedDeps.add(filtered.key, filtered.value);
                }
            }
            PropFile repos = new PropFile(Context.named("repositories"), PropFile.Loc.Local);
            for (String repoUrl : result.mavenRepositoryUrls) {
                String filteredRepoUrl = repoUrl;
                if (filteredRepoUrl.contains("${")) {
                    for (String mavenProperty : result.mavenProperties.keySet()) {
                        filteredRepoUrl = filter(filteredRepoUrl, mavenProperty, result.mavenProperties);
                    }
                }
                RepositoryAtom repoAtom = RepositoryAtom.parse("maven:" + filteredRepoUrl);
                repos.add(repoAtom.getPropertyName(), repoAtom.getPropertyValue());
            }
            PropFile modules = new PropFile(Context.named("submodules"), PropFile.Loc.Local);
            for (String module : result.modules) {
                modules.add(module, "");
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

    private FilterPair filterMavenDependency(FilterPair filterPair, Map<String, String> mavenProperties) {
        String filteredKey = filterPair.key;
        String filteredValue = filterPair.value;
        String originalKey = filteredKey;
        String originalValue = filteredValue;
        if (originalKey.contains("${") || originalValue.contains("${")) {
            for (String mavenProperty : mavenProperties.keySet()) {
                filteredKey = filter(filteredKey, mavenProperty, mavenProperties);
                filteredValue = filter(filteredValue, mavenProperty, mavenProperties);
            }
        }
        FilterPair filtered = new FilterPair(filteredKey, filteredValue);
        if ((!filteredKey.equals(originalKey) && filteredKey.contains("${"))
            || (!filteredValue.equals(originalValue) && filteredValue.contains("${"))) {
            return filterMavenDependency(filtered, mavenProperties);
        }
        return filtered;
    }

    public ParseResult parse(String pomUrlPath, RepositoryAtom repositoryAtom)
            throws ParserConfigurationException, IOException, SAXException {
        ParseResult parseResult = new ParseResult();
        PomUri pomUri = new PomUri(null, pomUrlPath, false);
        parse(pomUri, repositoryAtom, parseResult);
        return parseResult;
    }

    private void parse(PomUri pomUri, RepositoryAtom repositoryAtom, ParseResult parseResult)
            throws ParserConfigurationException, IOException, SAXException {
        if ((pomUri.relativeUrl != null) && !pomUri.relativeUrl.isEmpty()) {
            try {
                parse(pomUri.relativeUrl, pomUri, repositoryAtom, parseResult);
                return;
            } catch (Throwable t) {
                // fall through to next parse case
            }
        }
        parse(pomUri.uri, pomUri, repositoryAtom, parseResult);
    }

    private void parse(String pomUrlPath, PomUri pomUri, RepositoryAtom repositoryAtom, ParseResult parseResult)
            throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> headers = repositoryAtom.getAuthHeaders();
        Resource pomResource = Resources.parse(pomUrlPath, headers);
        try {
            InputStream stream = pomResource.open();
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList pomChildren = document.getDocumentElement().getChildNodes();
            // store the parent pom url so that recursive processing is done after the entire current pom is analyzed
            // so that any local property filtering (i.e., version) can be done.
            PomUri parentPomUri = null;
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
                } else if ("groupId".equals(nodeName) && !"${parent.groupId}".equals(child.getTextContent().trim())) {
                    localGroupId = child.getTextContent().trim();
                } else if ("artifactId".equals(nodeName)) {
                    localArtifactId = child.getTextContent().trim();
                } else if ("version".equals(nodeName) && !"${parent.version}".equals(child.getTextContent().trim())) {
                    localVersion = Version.resolve(child.getTextContent().trim(), getMetadataBaseUrl(pomUrlPath), headers);
                } else if ("packaging".equals(nodeName)) {
                    packaging = child.getTextContent().trim();
                } else if ("parent".equals(nodeName)) { // parent
                    parentPomUri = parseParentPomUrlPath(child, repositoryAtom, pomUri, parentGroupId, parentVersion);
                } else if ("build".equals(nodeName)) {
                    parseBuild(child, parseResult);
                } else if (!pomUri.parsingAsParent && "modules".equals(nodeName)) {
                    parseModules(child, parseResult);
                }
            }
            if (!parseResult.mavenProperties.containsKey("project.groupId")) {
                parseResult.mavenProperties.put("project.groupId", (localGroupId != null ? localGroupId : parentGroupId.get()));
                parseResult.mavenProperties.put("pom.groupId", (localGroupId != null ? localGroupId : parentGroupId.get()));
            }
            if (!parseResult.mavenProperties.containsKey("project.artifactId")) { // don't override artifactId with parent.artifactId
                parseResult.mavenProperties.put("project.artifactId", localArtifactId);
                parseResult.mavenProperties.put("pom.artifactId", localArtifactId);
            }
            if (!parseResult.mavenProperties.containsKey("project.version")) {
                String version = (localVersion != null ? localVersion : parentVersion.get());
                version = filterVersion(version, parseResult);
                parseResult.mavenProperties.put("project.version", version);
                parseResult.mavenProperties.put("pom.version", version);
            }
            if (!parseResult.mavenProperties.containsKey("project.packaging")) {
                parseResult.mavenProperties.put("project.packaging", packaging);
            }
            if (parentPomUri != null) {
                // filter project.* so that they are not overridden by the recursion on parent
                filterLocalProjectProperties(parseResult);
                parse(parentPomUri, repositoryAtom, parseResult);
                String version = parseResult.mavenProperties.get("project.version");
                version = filterVersion(version, parseResult);
                parseResult.mavenProperties.put("project.version", version);
            }
        } finally {
            pomResource.close();
        }
    }

    private void parseMavenImport(ParseResult.Incomplete incomplete, ParseResult importParse,
                                  String pomUrlPath, RepositoryAtom repositoryAtom, ParseResult parseResult)
            throws ParserConfigurationException, IOException, SAXException {
        Map<String, String> headers = repositoryAtom.getAuthHeaders();
        Resource pomResource = Resources.parse(pomUrlPath, headers);
        try {
            InputStream stream = pomResource.open();
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stream);
            NodeList pomChildren = document.getDocumentElement().getChildNodes();
            for (int i = 0; i < pomChildren.getLength(); i++) {
                Node child = pomChildren.item(i);
                String nodeName = child.getNodeName();
                if ("dependencyManagement".equals(nodeName)) {
                    parseDependencyManagementForImport(incomplete, importParse, child, parseResult, repositoryAtom);
                }
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

    private void parseDependencyManagementForImport(ParseResult.Incomplete incomplete, ParseResult importParse, Node dependencyManagementNode,
                                                    ParseResult parseResult, RepositoryAtom repositoryAtom) {
        NodeList dependencyManagementChildren = dependencyManagementNode.getChildNodes();
        for (int i = 0; i < dependencyManagementChildren.getLength(); i++) {
            Node dependenciesNode = dependencyManagementChildren.item(i);
            if ("dependencies".equals(dependenciesNode.getNodeName())) {
                parseDependenciesForImport(incomplete, importParse, dependenciesNode, parseResult, repositoryAtom);
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
                    groupId = child.getTextContent().trim();
                } else if ("artifactId".equals(child.getNodeName())) {
                    artifactId = child.getTextContent().trim();
                } else if ("version".equals(child.getNodeName())) {
                    version = child.getTextContent().trim();
                } else if ("classifier".equals(child.getNodeName())) {
                    classifier = child.getTextContent().trim();
                } else if ("type".equals(child.getNodeName())) {
                    type = child.getTextContent().trim();
                } else if ("scope".equals(child.getNodeName())) {
                    scope = child.getTextContent().trim();
                } else if ("optional".equals(child.getNodeName())) {
                    optional = child.getTextContent().trim();
                } else if ("systemPath".equals(child.getNodeName())) {
                    systemPath = true;
                } else if ("exclusions".equals(dependencies.item(i).getNodeName())) {
                    // ply treats exclusions much differently than maven, balk here and force project to specify explicitly
                }
            }
            version = Version.resolve(version, getMetadataBaseUrl(repositoryAtom, groupId, artifactId), repositoryAtom.getAuthHeaders());
            // iterating child->parent, per maven, child overrides parent, only place in if not already
            // exists (hence !override).
            parseResult.addDep(groupId, artifactId, version, classifier, type, scope, optional, systemPath, false, resolutionOnly);
        }
    }

    private void parseDependenciesForImport(ParseResult.Incomplete incomplete, ParseResult importParse, Node dependenciesNode,
                                            ParseResult parseResult, RepositoryAtom repositoryAtom) {
        NodeList dependencies = dependenciesNode.getChildNodes();
        for (int i = 0; i < dependencies.getLength(); i++) {
            if (!"dependency".equals(dependencies.item(i).getNodeName())) {
                continue;
            }
            NodeList dependencyNode = dependencies.item(i).getChildNodes();
            String groupId = "", artifactId = "", version = "", classifier = "", type = "";
            for (int j = 0; j < dependencyNode.getLength(); j++) {
                Node child = dependencyNode.item(j);
                if ("groupId".equals(child.getNodeName())) {
                    groupId = child.getTextContent().trim();
                } else if ("artifactId".equals(child.getNodeName())) {
                    artifactId = child.getTextContent().trim();
                } else if ("version".equals(child.getNodeName())) {
                    version = child.getTextContent().trim();
                } else if ("classifier".equals(child.getNodeName())) {
                    classifier = child.getTextContent().trim();
                } else if ("type".equals(child.getNodeName())) {
                    type = child.getTextContent().trim();
                }
            }
            if (parseResult.containsDep(groupId, artifactId, classifier, type)) {
                version = Version.resolve(version, getMetadataBaseUrl(repositoryAtom, groupId, artifactId), repositoryAtom.getAuthHeaders());
                String filteredVersion = filterVersion(version, importParse);
                Output.print("^dbug^ Importing version %s for %s:%s (from import scope dependency %s:%s)", filteredVersion, groupId, artifactId, incomplete.groupId, incomplete.artifactId);
                parseResult.replaceVersion(groupId, artifactId, classifier, type, filteredVersion);
            }
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
                    repoUrl = child.getTextContent().trim();
                } else if ("layout".equals(child.getNodeName())) {
                    layout = child.getTextContent().trim();
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
            // TODO - all the following need to be filtered (but should be after all other parsing).
            if ("directory".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.directory", child.getTextContent().trim());
            } else if ("outputDirectory".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.outputDirectory", child.getTextContent().trim());
            } else if ("sourceDirectory".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.sourceDirectory", child.getTextContent().trim());
            } else if ("testOutputDirectory".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.testOutputDirectory", child.getTextContent().trim());
            } else if ("testSourceDirectory".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.testSourceDirectory", child.getTextContent().trim());
            } else if ("finalName".equals(nodeName)) {
                parseResult.mavenProperties.put("project.build.finalName", child.getTextContent().trim());
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
            filteredDependencyKey = filter(filteredDependencyKey, "pom.groupId", parseResult.mavenProperties);
            incomplete.groupId = filter(incomplete.groupId, "project.groupId", parseResult.mavenProperties);
            incomplete.groupId = filter(incomplete.groupId, "pom.groupId", parseResult.mavenProperties);
            incomplete.version = filter(incomplete.version, "project.version", parseResult.mavenProperties);
            incomplete.version = filter(incomplete.version, "pom.version", parseResult.mavenProperties);
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
            try {
                return toFilter.replaceAll(Pattern.quote("${" + filterValue + "}"), Matcher
                        .quoteReplacement(replacementMap.get(filterValue)));
            } catch (IllegalArgumentException iae) {
                Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", filterValue,
                        replacementMap.get(filterValue));
                Output.print(iae);
            }
        }
        return toFilter;
    }

    private static String filterVersion(String version, ParseResult parseResult) {
        // filter as some projects (incorrectly) make the parent version a property
        if (version.contains("${") && version.endsWith("}")) {
            String propertyKey = version.substring(2, (version.length() - 1));
            if (parseResult.mavenProperties.containsKey(propertyKey)) {
                version = filter(version, propertyKey, parseResult.mavenProperties);
            }
        }
        return version;
    }

    private void parseProperties(Node propertiesNode, ParseResult parseResult) {
        NodeList properties = propertiesNode.getChildNodes();
        for (int i = 0; i < properties.getLength(); i++) {
            Node child = properties.item(i);
            // iterating child->parent, per maven, child overrides parent, only place in if not already exists.
            if (!parseResult.mavenProperties.containsKey(child.getNodeName())) {
                parseResult.mavenProperties.put(child.getNodeName(), child.getTextContent().trim());
            }
        }
    }

    private PomUri parseParentPomUrlPath(Node parent, RepositoryAtom repositoryAtom, PomUri self,
                                         AtomicReference<String> parentGroupId, AtomicReference<String> parentVersion) {
        NodeList children = parent.getChildNodes();
        String groupId = "", artifactId = "", version = "", relativePath = "";
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("groupId".equals(child.getNodeName())) {
                groupId = child.getTextContent().trim();
                parentGroupId.set(groupId);
            } else if ("artifactId".equals(child.getNodeName())) {
                artifactId = child.getTextContent().trim();
            } else if ("version".equals(child.getNodeName())) {
                version = child.getTextContent().trim();
                parentVersion.set(version);
            } else if ("relativePath".equals(child.getNodeName())) {
                relativePath = child.getTextContent().trim();
            }
        }

        PomUri pomUri = createPomUriWithoutRelativePath(repositoryAtom, true, groupId, artifactId, version);
        parentVersion.set(Version.resolve(parentVersion.get(), getMetadataBaseUrl(pomUri.uri), repositoryAtom.getAuthHeaders()));

        if (relativePath.isEmpty()) {
            String currentDir = ((self.relativeUrl == null) || self.relativeUrl.isEmpty()) ? "../" : self.relativeUrl;
            relativePath = FileUtil.pathFromParts(currentDir, artifactId, "pom.xml");
        }

        return new PomUri(relativePath, pomUri.uri, true);
    }

    private PomUri createPomUriWithoutRelativePath(RepositoryAtom repositoryAtom, boolean parsingAsParent,
                                                   String groupId, String artifactId, String version) {
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
        return new PomUri(null, pomUrlPath, parsingAsParent);
    }

}
