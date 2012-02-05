package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.SystemExit;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        String currentDir = FileUtil.getCanonicalPath(new File("./"));
        try {
            String url;
            if (!currentDir.contains("java-scripts")) {
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
        if (!currentDir.contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-empty-1")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-empty-1")).toURI().toURL().toString();
        }
        Map<String, List<String>> updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(1, updateInstructions.size());
        assertEquals(0, updateInstructions.get("VERSIONS").size());

        if (!currentDir.contains("java-scripts")) {
            url = (new File("java-scripts/ply-update/src/test/resources/update-instr-test-1")).toURI().toURL().toString();
        } else {
            url = (new File("src/test/resources/update-instr-test-1")).toURI().toURL().toString();
        }
        updateInstructions = UpdateScript.downloadUpdateInstr(url);
        assertEquals(2, updateInstructions.size());
        assertEquals(0, updateInstructions.get("1.0_1").size());
        assertEquals(1, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));

        if (!currentDir.contains("java-scripts")) {
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

        if (!currentDir.contains("java-scripts")) {
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

    @Test
    public void updateProperty() {
        String currentDir = FileUtil.getCanonicalPath(new File("./"));
        File configDir;
        if (!currentDir.contains("java-scripts")) {
            configDir = new File("java-scripts/ply-update/src/test/resources/config");
        } else {
            configDir = new File("src/test/resources/config");
        }

        // test null
        try {
            UpdateScript.updateProperty(null, configDir);
            fail("Expected a SystemExit exception as the instruction was null.");
        } catch (SystemExit se) {
            assertNull(se.getCause());
        }

        // test invalid instruction - no context
        try {
            UpdateScript.updateProperty("property=value|expected", configDir);
            fail("Expected a SystemExit exception as the instruction has no context.");
        } catch (SystemExit se) {
            assertNull(se.getCause());
        }
        // test invalid instruction - no propName break
        try {
            UpdateScript.updateProperty("context.property:value|expected", configDir);
            fail("Expected a SystemExit exception as the instruction has no property name break '='.");
        } catch (SystemExit se) {
            assertNull(se.getCause());
        }
        // test invalid instruction - no propValue end pipe
        try {
            UpdateScript.updateProperty("context.property=value", configDir);
            fail("Expected a SystemExit exception as the instruction has no property value ending pipe '|'.");
        } catch (SystemExit se) {
            assertNull(se.getCause());
        }

        // test property name with invalid expected value
        int warnings = UpdateScript.updateProperty("aliases.update=ply-update-2.0.jar|something-else", configDir);
        assertEquals(1, warnings);
        Properties aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-1.0.jar", aliases.getProperty("update"));
        // test property name with valid expected value
        warnings = UpdateScript.updateProperty("aliases.update=ply-update-2.0.jar|ply-update-1.0.jar", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-2.0.jar", aliases.getProperty("update"));

        // test property name with invalid expected value
        warnings = UpdateScript.updateProperty("aliases.update=ply-update-3.0.jar|", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-2.0.jar", aliases.getProperty("update"));

        // test property name with a period in it
        // test property name with invalid expected value
        warnings = UpdateScript.updateProperty("aliases.user.modified=testing|blah", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("something-else", aliases.getProperty("user.modified"));
        // test property name with valid expected value
        warnings = UpdateScript.updateProperty("aliases.user.modified=testing|something-else", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("testing", aliases.getProperty("user.modified"));

        // test property value with pipe in it
        warnings = UpdateScript.updateProperty("aliases.user.modified=with\\| character|testing", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("with| character", aliases.getProperty("user.modified"));
        warnings = UpdateScript.updateProperty("aliases.user.modified=\\|pipe!|with| character", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("|pipe!", aliases.getProperty("user.modified"));

        // test property where the user has deleted the existing
        warnings = UpdateScript.updateProperty("aliases.user.deleted=something|dne", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertFalse(aliases.containsKey("user.deleted"));

        // test add a new property
        warnings = UpdateScript.updateProperty("aliases.system.created=something|", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("something", aliases.getProperty("system.created"));

        // test the properties file dne
        warnings = UpdateScript.updateProperty("project.prop=newvalue|oldvalue", configDir);
        assertEquals(1, warnings);
        Properties project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.properties"), false, true);
        assertNull(project);

        // test the properties file dne but is created
        warnings = UpdateScript.updateProperty("project.prop=newvalue|", configDir);
        assertEquals(0, warnings);
        project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.properties"));
        assertEquals("newvalue", project.getProperty("prop"));

        // test scoped properties file, creation
        warnings = UpdateScript.updateProperty("project#test.testprop=newvalue|", configDir);
        assertEquals(0, warnings);
        project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.test.properties"));
        assertEquals("newvalue", project.getProperty("testprop"));
    }

    @After
    public void teardown() {
        String currentDir = FileUtil.getCanonicalPath(new File("./"));
        File configDir;
        if (!currentDir.contains("java-scripts")) {
            configDir = new File("java-scripts/ply-update/src/test/resources/config");
        } else {
            configDir = new File("src/test/resources/config");
        }
        String aliasesFile = FileUtil.pathFromParts(configDir.getPath(), "aliases.properties");
        Properties aliases = PropertiesFileUtil.load(aliasesFile);
        aliases.clear();
        aliases.put("update", "ply-update-1.0.jar");
        aliases.put("user.modified", "something-else");
        PropertiesFileUtil.store(aliases, aliasesFile);

        FileUtil.delete(FileUtil.fromParts(configDir.getPath(), "project.properties"));
        FileUtil.delete(FileUtil.fromParts(configDir.getPath(), "project.test.properties"));
    }

}
