package net.ocheyedan.ply.input;

import net.ocheyedan.ply.FileUtil;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 11/19/11
 * Time: 8:26 AM
 */
public class ResourcesTest {

    @Test
    public void parse() throws IOException {

        File testFile = File.createTempFile("resources", "test");

        // test file
        Resource resource = Resources.parse(testFile.getPath(), null);
        try {
            resource.open();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            resource.close();
        }

        // test ~
        File withinHome = new File(FileUtil.pathFromParts(System.getProperty("user.home"), "test.txt"));
        withinHome.createNewFile();
        withinHome.deleteOnExit();
        resource = Resources.parse(FileUtil.pathFromParts("~", "test.txt"), null);
        try {
            resource.open();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            resource.close();
        }

        // test uri-syntax file
        resource = Resources.parse("file://" + testFile.getPath(), null);
        try {
            resource.open();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            resource.close();
        }

        // test uri - http:
        resource = Resources.parse("http://repo1.maven.org/maven2/commons-lang/commons-lang/2.6/commons-lang-2.6.pom", null);
        try {
            resource.open();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            resource.close();
        }

        // test classpath:
        resource = Resources.parse("classpath:mock-mvn-repo/commons-logging/commons-logging/1.1.1/maven-metadata.xml", null);
        try {
            resource.open();
        } catch (IOException ioe) {
            fail(ioe.getMessage());
        } finally {
            resource.close();
        }
    }

}
