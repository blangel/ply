package net.ocheyedan.ply.mvn;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 11/11/11
 * Time: 7:22 AM
 */
public class VersionTest {

    @Test
    public void mavenVersionComparator() {
        List<String> versions = new ArrayList<String>();
        versions.add("1.0");
        versions.add("2.0.4-SNAPSHOT-1");
        versions.add("2.0.4-SNAPSHOT-0");
        versions.add("1.0-SNAPSHOT");
        versions.add("2.0.3");
        versions.add("2.0.4");
        versions.add("2.0-RELEASE");
        Collections.sort(versions, Version.MAVEN_VERSION_COMPARATOR);
        assertEquals("1.0-SNAPSHOT", versions.get(0));
        assertEquals("1.0", versions.get(1));
        assertEquals("2.0-RELEASE", versions.get(2));
        assertEquals("2.0.3", versions.get(3));
        assertEquals("2.0.4-SNAPSHOT-0", versions.get(4));
        assertEquals("2.0.4-SNAPSHOT-1", versions.get(5));
        assertEquals("2.0.4", versions.get(6));
    }

    @Test
    public void resolve() {

        assertNull(Version.resolve(null, null));

        String version = "1.0";
        assertEquals(version, Version.resolve(version, null));
        version = "1.0-SNAPSHOT";
        assertEquals(version, Version.resolve(version, null));
        version = "3.0.5.RELEASE";
        assertEquals(version, Version.resolve(version, null));

        version = "[, 1.0 ]";
        assertEquals("1.0", Version.resolve(version, null));
        version = "(,1.0]";
        assertEquals("1.0", Version.resolve(version, null));
        version = "[0.1,1.0]";
        assertEquals("1.0", Version.resolve(version, null));
        version = "(0.1,1.0]";
        assertEquals("1.0", Version.resolve(version, null));
        version = "[,1.0-SNAPSHOT]";
        assertEquals("1.0-SNAPSHOT", Version.resolve(version, null));
        version = "(,1.0-SNAPSHOT]";
        assertEquals("1.0-SNAPSHOT", Version.resolve(version, null));
        version = "[0.1,1.0-SNAPSHOT]";
        assertEquals("1.0-SNAPSHOT", Version.resolve(version, null));
        version = "(0.1,1.0-SNAPSHOT]";
        assertEquals("1.0-SNAPSHOT", Version.resolve(version, null));


        version = "[,1.4.3)";
        assertEquals("1.4.3-rc1", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(,1.4.3)";
        assertEquals("1.4.3-rc1", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.3-rc1,1.4.3)";
        assertEquals("1.4.3-rc1", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "( 1.4.3-rc1 ,1.4.3)";
        assertNull(Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[,1.4.4-rc1)";
        assertEquals("1.4.3", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(,1.4.4-rc1)";
        assertEquals("1.4.3", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.4-rc1,1.4.4)";
        assertEquals("1.4.4-rc1", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.4,1.4.4)";
        assertNull(Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));

        version = "[1.4.2,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.2,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.3-rc1,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.3-rc1,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.3,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.3,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.4-rc1,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.4-rc1,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "[1.4.4,)";
        assertEquals("1.4.4", Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));
        version = "(1.4.4,)";
        assertNull(Version.resolve(version, "classpath:mock-mvn-repo/javax/mail/mail"));

        version = "[1.0,2.0),[3.0,)";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.UnsupportedRangeSet exception");
        } catch (Version.UnsupportedRangeSet vurs) {
            // expected
        }
        version = "(1.0,2.0],(3.0,]";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.UnsupportedRangeSet exception");
        } catch (Version.UnsupportedRangeSet vurs) {
            // expected
        }
        version = "(,1.0],[1.2,)";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.UnsupportedRangeSet exception");
        } catch (Version.UnsupportedRangeSet vurs) {
            // expected
        }
        version = "(,1.1),(1.1,)";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.UnsupportedRangeSet exception");
        } catch (Version.UnsupportedRangeSet vurs) {
            // expected
        }

        version = "(,)";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.Invalid exception");
        } catch (Version.Invalid vurs) {
            // expected
        }
        version = "(,]";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.Invalid exception");
        } catch (Version.Invalid vurs) {
            // expected
        }
        version = "[,)";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.Invalid exception");
        } catch (Version.Invalid vurs) {
            // expected
        }
        version = "[,]";
        try {
            Version.resolve(version, null);
            fail("Expecting a Version.Invalid exception");
        } catch (Version.Invalid vurs) {
            // expected
        }

    }


}
