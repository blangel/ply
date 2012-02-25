package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.PropFile;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 11/17/11
 * Time: 8:23 PM
 */
public class MavenPomParserTest {

    @Test
    public void parsePom() throws URISyntaxException {
        MavenPomParser parser = new MavenPomParser();
        RepositoryAtom mockRepo = new RepositoryAtom(new URI("classpath:mock-mvn-repo/"));
        MavenPom pom = parser.parsePom("classpath:mock-mvn-repo/log4j/log4j/1.2.16/log4j-1.2.16.pom", mockRepo);
        assertEquals("log4j", pom.groupId);
        assertEquals("log4j", pom.artifactId);
        assertEquals("1.2.16", pom.version);
        assertEquals("bundle", pom.packaging);
        PropFile properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(2, properties.size());
        String javaxMail = properties.get("javax.mail:mail").value();
        assertEquals("1.4.1:transient", javaxMail);
        String geronimoJmsSpec = properties.get("org.apache.geronimo.specs:geronimo-jms_1.1_spec").value();
        assertEquals("1.0:transient", geronimoJmsSpec);
        properties = pom.testDependencies;
        assertNotNull(properties);
        assertEquals(2, properties.size());
        String junit = properties.get("junit:junit").value();
        assertEquals("3.8.2", junit);
        String oro = properties.get("oro:oro").value();
        assertEquals("2.0.8", oro);

        pom = parser.parsePom("classpath:mock-mvn-repo/com/amazonaws/aws-java-sdk/1.2.12/aws-java-sdk-1.2.12.pom", mockRepo);
        assertEquals("com.amazonaws", pom.groupId);
        assertEquals("aws-java-sdk", pom.artifactId);
        assertEquals("1.2.12", pom.version);
        assertEquals("jar", pom.packaging);
        properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(7, properties.size());
        String commonsCodec = properties.get("commons-codec:commons-codec").value();
        assertEquals("1.4", commonsCodec);
        String jacksonCoreAsl = properties.get("org.codehaus.jackson:jackson-core-asl").value();
        assertEquals("1.9.2", jacksonCoreAsl);
        String httpClient = properties.get("org.apache.httpcomponents:httpclient").value();
        assertEquals("4.2-alpha1", httpClient);
        String staxApi = properties.get("stax:stax-api").value();
        assertEquals("1.0.1", staxApi);
        javaxMail = properties.get("javax.mail:mail").value();
        assertEquals("1.4.4", javaxMail);
        String commonsLogging = properties.get("commons-logging:commons-logging").value();
        assertEquals("1.1.1", commonsLogging);
        String stax = properties.get("stax:stax").value();
        assertEquals("1.2.0:transient", stax);
        assertTrue(pom.testDependencies.isEmpty());

        pom = parser.parsePom("classpath:mock-mvn-repo/org/apache/httpcomponents/httpcomponents-client/4.2-alpha1.1/httpcomponents-client-4.2-alpha1.1.pom", mockRepo);
        assertEquals("org.apache.httpcomponents", pom.groupId);
        assertEquals("httpcomponents-client", pom.artifactId);
        assertEquals("4.2-alpha1.1", pom.version);
        assertEquals("pom", pom.packaging);
        properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(6, properties.size());
        commonsCodec = properties.get("commons-codec:commons-codec").value();
        assertEquals("1.4", commonsCodec);
        String ehcacheCore = properties.get("net.sf.ehcache:ehcache-core").value();
        assertEquals("2.2.0", ehcacheCore);
        String httpcore = properties.get("org.apache.httpcomponents:httpcore").value();
        assertEquals("4.2-alpha2", httpcore);
        commonsLogging = properties.get("commons-logging:commons-logging").value();
        assertEquals("1.1.1", commonsLogging);
        String slf4jJcl = properties.get("org.slf4j:slf4j-jcl").value();
        assertEquals("1.5.11", slf4jJcl);
        String spymemcached = properties.get("spy:spymemcached").value();
        assertEquals("2.6", spymemcached);
        assertEquals(4, pom.testDependencies.size());
        properties = pom.testDependencies;
        junit = properties.get("junit:junit").value();
        assertEquals("4.9", junit);
        String mockitoCore = properties.get("org.mockito:mockito-core").value();
        assertEquals("1.8.5", mockitoCore);
        String easymock = properties.get("org.easymock:easymock").value();
        assertEquals("2.5.2", easymock);
        String easymockClassextension = properties.get("org.easymock:easymockclassextension").value();
        assertEquals("2.5.2", easymockClassextension);

        properties = pom.repositories;
        assertEquals(2, properties.size());
        assertEquals("maven", properties.get("http://files.couchbase.com/maven2/").value());
        assertEquals("maven", properties.get("http://repository.apache.org/snapshots").value());

        properties = pom.modules;
        assertEquals(5, properties.size());
        assertEquals("", properties.get("httpclient").value());
        assertEquals("", properties.get("httpmime").value());
        assertEquals("", properties.get("httpclient-cache").value());
        assertEquals("", properties.get("fluent-hc").value());
        assertEquals("", properties.get("httpclient-osgi").value());
    }

}
