package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.dep.RepositoryAtom;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * User: blangel
 * Date: 11/17/11
 * Time: 8:23 PM
 */
public class MavenPomParserTest {

    @Test
    public void parsePom() throws URISyntaxException {
        MavenPomParser parser = new MavenPomParser.Default();
        RepositoryAtom mockRepo = new RepositoryAtom(new URI("classpath:mock-mvn-repo/"));
        MavenPom pom = parser.parsePom("classpath:mock-mvn-repo/log4j/log4j/1.2.16/log4j-1.2.16.pom", mockRepo);
        assertEquals("log4j", pom.groupId);
        assertEquals("log4j", pom.artifactId);
        assertEquals("1.2.16", pom.version);
        assertEquals("bundle", pom.packaging);
        Properties properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(2, properties.size());
        String javaxMail = properties.getProperty("javax.mail:mail");
        assertEquals("1.4.1:transient", javaxMail);
        String geronimoJmsSpec = properties.getProperty("org.apache.geronimo.specs:geronimo-jms_1.1_spec");
        assertEquals("1.0:transient", geronimoJmsSpec);
        properties = pom.testDependencies;
        assertNotNull(properties);
        assertEquals(2, properties.size());
        String junit = properties.getProperty("junit:junit");
        assertEquals("3.8.2", junit);
        String oro = properties.getProperty("oro:oro");
        assertEquals("2.0.8", oro);

        pom = parser.parsePom("classpath:mock-mvn-repo/com/amazonaws/aws-java-sdk/1.2.12/aws-java-sdk-1.2.12.pom", mockRepo);
        assertEquals("com.amazonaws", pom.groupId);
        assertEquals("aws-java-sdk", pom.artifactId);
        assertEquals("1.2.12", pom.version);
        assertEquals("jar", pom.packaging);
        properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(7, properties.size());
        String commonsCodec = properties.getProperty("commons-codec:commons-codec");
        assertEquals("1.4", commonsCodec);
        String jacksonCoreAsl = properties.getProperty("org.codehaus.jackson:jackson-core-asl");
        assertEquals("1.9.2", jacksonCoreAsl);
        String httpClient = properties.getProperty("org.apache.httpcomponents:httpclient");
        assertEquals("4.2-alpha1", httpClient);
        String staxApi = properties.getProperty("stax:stax-api");
        assertEquals("1.0.1", staxApi);
        javaxMail = properties.getProperty("javax.mail:mail");
        assertEquals("1.4.4", javaxMail);
        String commonsLogging = properties.getProperty("commons-logging:commons-logging");
        assertEquals("1.1.1", commonsLogging);
        String stax = properties.getProperty("stax:stax");
        assertEquals("1.2.0:transient", stax);
        assertTrue(pom.testDependencies.isEmpty());

        pom = parser.parsePom("classpath:mock-mvn-repo/org/apache/httpcomponents/httpcomponents-client/4.2-alpha1.1/httpcomponents-client-4.2-alpha1.1.pom", mockRepo);
        assertEquals("org.apache.httpcomponents", pom.groupId);
        assertEquals("httpcomponents-client", pom.artifactId);
        assertEquals("4.2-alpha1.1", pom.version);
        assertEquals("jar", pom.packaging);
        properties = pom.dependencies;
        assertNotNull(properties);
        assertEquals(6, properties.size());
        commonsCodec = properties.getProperty("commons-codec:commons-codec");
        assertEquals("1.4", commonsCodec);
        String ehcacheCore = properties.getProperty("net.sf.ehcache:ehcache-core");
        assertEquals("2.2.0", ehcacheCore);
        String httpcore = properties.getProperty("org.apache.httpcomponents:httpcore");
        assertEquals("4.2-alpha2", httpcore);
        commonsLogging = properties.getProperty("commons-logging:commons-logging");
        assertEquals("1.1.1", commonsLogging);
        String slf4jJcl = properties.getProperty("org.slf4j:slf4j-jcl");
        assertEquals("1.5.11", slf4jJcl);
        String spymemcached = properties.getProperty("spy:spymemcached");
        assertEquals("2.6", spymemcached);
        assertEquals(4, pom.testDependencies.size());
        properties = pom.testDependencies;
        junit = properties.getProperty("junit:junit");
        assertEquals("4.9", junit);
        String mockitoCore = properties.getProperty("org.mockito:mockito-core");
        assertEquals("1.8.5", mockitoCore);
        String easymock = properties.getProperty("org.easymock:easymock");
        assertEquals("2.5.2", easymock);
        String easymockClassextension = properties.getProperty("org.easymock:easymockclassextension");
        assertEquals("2.5.2", easymockClassextension);

        properties = pom.repositories;
        assertEquals(2, properties.size());
        assertEquals("maven", properties.getProperty("http://files.couchbase.com/maven2/"));
        assertEquals("maven", properties.getProperty("http://repository.apache.org/snapshots"));
    }

}
