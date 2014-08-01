package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.input.Resource;
import net.ocheyedan.ply.input.Resources;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * User: blangel
 * Date: 11/11/11
 * Time: 8:56 AM
 *
 * Responsible for parsing {@literal Maven} metadata files.
 */
public class MavenMetadataParser {

    public static final class Metadata {

        public final String latest;

        public final List<String> versions;

        public Metadata(String latest, List<String> versions) {
            this.latest = latest;
            this.versions = versions;
        }
    }

    public Metadata parseMetadata(String baseUrl, Map<String, String> headers) {
        if (baseUrl == null) {
            return null;
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        try {
            return parse(baseUrl + "maven-metadata.xml", headers);
        } catch (IOException ioe) {
            try {
                return parse(baseUrl + "metadata.xml", headers);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Metadata parse(String metadataXmlUrl, Map<String, String> headers) throws IOException, ParserConfigurationException, SAXException {
        Resource resource = Resources.parse(metadataXmlUrl, headers);
        try {
            return parse(resource);
        } finally {
            resource.close();
        }
    }

    private Metadata parse(Resource resource) throws IOException, ParserConfigurationException, SAXException {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resource.open());
        NodeList metadataChildren = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < metadataChildren.getLength(); i++) {
            Node child = metadataChildren.item(i);
            String nodeName = child.getNodeName();
            if ("versioning".equals(nodeName)) {
                return parseVersioning(child);
            }
        }
        throw new AssertionError(String.format("Could not parse metadata file %s.", resource.name()));
    }

    private Metadata parseVersioning(Node versioning) {
        String latest = null;
        List<String> versions = null;

        NodeList versioningChildren = versioning.getChildNodes();
        for (int i = 0; i < versioningChildren.getLength(); i++) {
            Node child = versioningChildren.item(i);
            if ("latest".equals(child.getNodeName())) {
                latest = child.getTextContent();
            } else if ("versions".equals(child.getNodeName())) {
                versions = parseVersions(child);
            }
        }

        return new Metadata(latest, versions);
    }

    private List<String> parseVersions(Node versionsNode) {
        NodeList versionsChildren = versionsNode.getChildNodes();
        List<String> versions = new ArrayList<String>(versionsChildren.getLength());
        for (int i = 0; i < versionsChildren.getLength(); i++) {
            Node version = versionsChildren.item(i);
            if (!"version".equals(version.getNodeName())) {
                continue;
            }
            versions.add(version.getTextContent());
        }
        Collections.sort(versions, Version.MAVEN_VERSION_COMPARATOR);
        return versions;
    }
}
