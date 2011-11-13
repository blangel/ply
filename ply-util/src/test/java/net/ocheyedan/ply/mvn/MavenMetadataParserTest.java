package net.ocheyedan.ply.mvn;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * User: blangel
 * Date: 11/11/11
 * Time: 9:25 AM
 */
public class MavenMetadataParserTest {

    @Test
    public void parseMetadata() {
        MavenMetadataParser.Default parser = new MavenMetadataParser.Default();
        assertNull(parser.parseMetadata(null));

        MavenMetadataParser.Metadata metadata = parser.parseMetadata("classpath:mock-mvn-repo/log4j/log4j");
        assertEquals("1.2.16", metadata.latest);
        assertEquals(1, metadata.versions.size());
        assertEquals("1.2.16", metadata.versions.get(0));
        metadata = parser.parseMetadata("classpath:mock-mvn-repo/log4j/log4j/"); // test with ending slash
        assertEquals("1.2.16", metadata.latest);
        assertEquals(1, metadata.versions.size());
        assertEquals("1.2.16", metadata.versions.get(0));

        metadata = parser.parseMetadata("classpath:mock-mvn-repo/javax/mail/mail");
        assertEquals("1.4.4", metadata.latest);
        assertEquals(5, metadata.versions.size());
        assertEquals("1.4.2", metadata.versions.get(0));
        assertEquals("1.4.3-rc1", metadata.versions.get(1));
        assertEquals("1.4.3", metadata.versions.get(2));
        assertEquals("1.4.4-rc1", metadata.versions.get(3));
        assertEquals("1.4.4", metadata.versions.get(4));
    }

}
