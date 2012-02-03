package net.ocheyedan.ply.script;

import net.ocheyedan.ply.SystemExit;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 2/2/12
 * Time: 11:33 AM
 */
public class UpdateScriptTest {

    @Test
    public void downloadUpdateInstr() throws MalformedURLException {
        // test an invalid url
        try {
            String url = "not a url";
            UpdateScript.downloadUpdateInstr(url);
            fail(String.format("Expected a SystemExit exception as %s is not a url.", url));
        } catch (SystemExit se) {
            assertNotNull(se.getCause());
        }
        // test an valid url but which doesn't exist
        try {
            String url = "file://src/test/resources/doesNotExist";
            UpdateScript.downloadUpdateInstr(url);
            fail(String.format("Expected a SystemExit exception as %s does not exist.", url));
        } catch (SystemExit se) {
            assertNotNull(se.getCause());
        }

        // test an invalid update-instructions file
        File currentDir = new File("./");
        try {
            String url;
            if (!currentDir.toURI().toURL().toString().contains("java-scripts")) {
                url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-fail-1")).toURI().toURL().toString();
            } else {
                url = (new File("src/test/resources/update-instr-test-fail-1")).toURI().toURL().toString();
            }
            UpdateScript.downloadUpdateInstr(url);
            fail(String.format("Expected a SystemExit exception as %s is in an invalid format.", url));
        } catch (SystemExit se) {
            assertNull(se.getCause());
        }

        // check empty file
        String url;
        if (!currentDir.toURI().toURL().toString().contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-empty-1")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-empty-1")).toURI().toURL().toString();
        }
        Map<String, List<String>> updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(1, updateInstructions.size());
        assertEquals(0, updateInstructions.get("VERSIONS").size());

        if (!currentDir.toURI().toURL().toString().contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-1")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-1")).toURI().toURL().toString();
        }
        updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(2, updateInstructions.size());
        assertEquals(0, updateInstructions.get("1.0_1").size());
        assertEquals(1, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));

        if (!currentDir.toURI().toURL().toString().contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-2")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-2")).toURI().toURL().toString();
        }
        updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(3, updateInstructions.size());
        assertEquals(2, updateInstructions.get("1.0_1").size());
        assertEquals("do something", updateInstructions.get("1.0_1").get(0));
        assertEquals("something else", updateInstructions.get("1.0_1").get(1)); // note the trim
        assertEquals(1, updateInstructions.get("1.0_2").size());
        assertEquals("next version do something", updateInstructions.get("1.0_2").get(0));
        assertEquals(2, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));
        assertEquals("1.0_2", updateInstructions.get("VERSIONS").get(1));

        if (!currentDir.toURI().toURL().toString().contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-3")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-3")).toURI().toURL().toString();
        }
        updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(4, updateInstructions.size());
        assertEquals(0, updateInstructions.get("1.0_1").size());
        assertEquals(1, updateInstructions.get("1.0_2").size());
        assertEquals("something to do", updateInstructions.get("1.0_2").get(0));
        assertEquals(0, updateInstructions.get("1.0_3").size());
        assertEquals(3, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));
        assertEquals("1.0_2", updateInstructions.get("VERSIONS").get(1));
        assertEquals("1.0_3", updateInstructions.get("VERSIONS").get(2));

    }

}
