package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.dep.RepositoryAtom;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

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
        Properties properties = parser.parsePom("classpath:mock-mvn-repo/log4j/log4j/1.2.16/log4j-1.2.16.pom", mockRepo);
        assertNotNull(properties);
        assertEquals(2, properties.size());
        String javaxMail = properties.getProperty("javax.mail:mail");
        assertEquals("1.4.1:transient", javaxMail);
        String geronimoJmsSpec = properties.getProperty("org.apache.geronimo.specs:geronimo-jms_1.1_spec");
        assertEquals("1.0:transient", geronimoJmsSpec);

        properties = parser.parsePom("classpath:mock-mvn-repo/com/amazonaws/aws-java-sdk/1.2.12/aws-java-sdk-1.2.12.pom", mockRepo);
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
    }

}
