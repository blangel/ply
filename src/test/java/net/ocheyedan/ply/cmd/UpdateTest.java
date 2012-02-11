package net.ocheyedan.ply.cmd;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.SystemExit;
import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static junit.framework.Assert.*;
import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 2/11/12
 * Time: 11:00 AM
 */
public class UpdateTest {

    @Test
    @SuppressWarnings("unchecked")
    public void downloadUpdateInstr() throws MalformedURLException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        Method downloadUpdateInstrMethod = Update.class.getDeclaredMethod("downloadUpdateInstr", String.class);
        downloadUpdateInstrMethod.setAccessible(true);
        Update update = new Update(new Args(Collections.<String>emptyList(), Collections.<String>emptyList()));
        // test an invalid url
        try {
            String url = "not a url";
            downloadUpdateInstrMethod.invoke(update, url);
            fail(String.format("Expected a SystemExit exception as %s is not a url.", url));
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNotNull(se.getCause());
        }
        // test an valid url but which doesn't exist
        try {
            String url = "file://src/test/resources/doesNotExist";
            downloadUpdateInstrMethod.invoke(update, url);
            fail(String.format("Expected a SystemExit exception as %s does not exist.", url));
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNotNull(se.getCause());
        }

        // test an invalid update-instructions file
        String currentDir = FileUtil.getCanonicalPath(new File("./"));
        try {
            String url = (new File("src/test/resources/update-instr-test-fail-1")).toURI().toURL().toString();
            downloadUpdateInstrMethod.invoke(update, url);
            fail(String.format("Expected a SystemExit exception as %s is in an invalid format.", url));
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }

        // check empty file
        String url= (new File("src/test/resources/update-instr-test-empty-1")).toURI().toURL().toString();
        Map<String, List<String>> updateInstructions = (Map<String, List<String>>) downloadUpdateInstrMethod.invoke(update, url);
        assertEquals(1, updateInstructions.size());
        assertEquals(0, updateInstructions.get("VERSIONS").size());

        url = (new File("src/test/resources/update-instr-test-1")).toURI().toURL().toString();
        updateInstructions = (Map<String, List<String>>) downloadUpdateInstrMethod.invoke(update, url);
        assertEquals(2, updateInstructions.size());
        assertEquals(0, updateInstructions.get("1.0_1").size());
        assertEquals(1, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));

        url = (new File("src/test/resources/update-instr-test-2")).toURI().toURL().toString();
        updateInstructions = (Map<String, List<String>>) downloadUpdateInstrMethod.invoke(update, url);
        assertEquals(3, updateInstructions.size());
        assertEquals(2, updateInstructions.get("1.0_1").size());
        assertEquals("do something", updateInstructions.get("1.0_1").get(0));
        assertEquals("something else", updateInstructions.get("1.0_1").get(1)); // note the trim
        assertEquals(1, updateInstructions.get("1.0_2").size());
        assertEquals("next version do something", updateInstructions.get("1.0_2").get(0));
        assertEquals(2, updateInstructions.get("VERSIONS").size());
        assertEquals("1.0_1", updateInstructions.get("VERSIONS").get(0));
        assertEquals("1.0_2", updateInstructions.get("VERSIONS").get(1));

        url = (new File("src/test/resources/update-instr-test-3")).toURI().toURL().toString();
        updateInstructions = (Map<String, List<String>>) downloadUpdateInstrMethod.invoke(update, url);
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
    public void download() throws NoSuchMethodException, IllegalAccessException, MalformedURLException,
            InvocationTargetException {
        Method downloadMethod = Update.class.getDeclaredMethod("download", String.class, File.class);
        downloadMethod.setAccessible(true);
        File configDir = new File("src/test/resources/update-ply-home");
        Update update = new Update(new Args(null, null));
        
        // test invalid formats
        try {
            downloadMethod.invoke(update, null, configDir);
            fail("Expecting an exception as null was passed into the download method as the instruction.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        try {
            downloadMethod.invoke(update, "does not contain the necessary delimiter", configDir);
            fail("Expecting an exception as the instruction passed into the download method was an invalid format.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        try {
            downloadMethod.invoke(update, "something to ", configDir);
            fail("Expecting an exception as the instruction passed into the download method did not have a location.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        try {
            downloadMethod.invoke(update, " to ", configDir);
            fail("Expecting an exception as the instruction passed into the download method did not have a file specified.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        try {
            downloadMethod.invoke(update, "dot a valid url to dne.txt", configDir);
            fail("Expecting an exception as the instruction passed into the download method did not have a valid file.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNotNull(se.getCause());
            assertTrue(se.getCause() instanceof MalformedURLException);
        }
        try {
            File dne = FileUtil.fromParts(configDir.getPath(), "dne.txt");
            downloadMethod.invoke(update, String.format("%s to copied/dne.txt", dne.toURI().toURL().toString()), configDir);
            fail("Expecting an exception as the instruction passed into the download method did not have a file which exists.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNotNull(se.getCause());
            assertTrue(se.getCause() instanceof FileNotFoundException);
        }

        // test successful case
        File copiedAliasesProperties = FileUtil.fromParts(configDir.getPath(), "aliases.properties");
        assertFalse(copiedAliasesProperties.exists());
        File aliasesProperties = new File("src/test/resources/update-config/aliases.properties");
        downloadMethod.invoke(update, String.format("%s to aliases.properties", aliasesProperties.toURI().toURL().toString()), configDir);
        copiedAliasesProperties = FileUtil.fromParts(configDir.getPath(), "aliases.properties");
        assertTrue(copiedAliasesProperties.exists());

        // test successful case where subdirectories are created
        File copiedNestedAliasesProperties = FileUtil.fromParts(configDir.getPath(), "nest", "aliases.properties");
        assertFalse(copiedNestedAliasesProperties.exists());
        downloadMethod.invoke(update, String.format("%s to nest/aliases.properties", aliasesProperties.toURI().toURL().toString()), configDir);
        copiedNestedAliasesProperties = FileUtil.fromParts(configDir.getPath(), "nest", "aliases.properties");
        assertTrue(copiedNestedAliasesProperties.exists());
    }

    @Test
    public void updateProperty() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Method updatePropertyMethod = Update.class.getDeclaredMethod("updateProperty", String.class, File.class);
        updatePropertyMethod.setAccessible(true);
        Update update = new Update(new Args(null, null));
        
        File configDir = new File("src/test/resources/update-config");
        // test null
        try {
            updatePropertyMethod.invoke(update, null, configDir);
            fail("Expected a SystemExit exception as the instruction was null.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }

        // test invalid instruction - no context
        try {
            updatePropertyMethod.invoke(update, "property=value|expected", configDir);
            fail("Expected a SystemExit exception as the instruction has no context.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        // test invalid instruction - no propName break
        try {
            updatePropertyMethod.invoke(update, "context.property:value|expected", configDir);
            fail("Expected a SystemExit exception as the instruction has no property name break '='.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }
        // test invalid instruction - no propValue end pipe
        try {
            updatePropertyMethod.invoke(update, "context.property=value", configDir);
            fail("Expected a SystemExit exception as the instruction has no property value ending pipe '|'.");
        } catch (InvocationTargetException ite) {
            assertTrue(ite.getCause() instanceof SystemExit);
            SystemExit se = (SystemExit) ite.getCause();
            assertNull(se.getCause());
        }

        // test property name with invalid expected value
        int warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.update=ply-update-2.0.jar|something-else", configDir);
        assertEquals(1, warnings);
        Properties aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-1.0.jar", aliases.getProperty("update"));
        // test property name with valid expected value
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.update=ply-update-2.0.jar|ply-update-1.0.jar", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-2.0.jar", aliases.getProperty("update"));

        // test property name with invalid expected value
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.update=ply-update-3.0.jar|", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("ply-update-2.0.jar", aliases.getProperty("update"));

        // test property name with a period in it
        // test property name with invalid expected value
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.user.modified=testing|blah", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("something-else", aliases.getProperty("user.modified"));
        // test property name with valid expected value
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.user.modified=testing|something-else", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("testing", aliases.getProperty("user.modified"));

        // test property value with pipe in it
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.user.modified=with\\| character|testing", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("with| character", aliases.getProperty("user.modified"));
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.user.modified=\\|pipe!|with| character", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("|pipe!", aliases.getProperty("user.modified"));

        // test property where the user has deleted the existing
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.user.deleted=something|dne", configDir);
        assertEquals(1, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertFalse(aliases.containsKey("user.deleted"));

        // test add a new property
        warnings = (Integer) updatePropertyMethod.invoke(update, "aliases.system.created=something|", configDir);
        assertEquals(0, warnings);
        aliases = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "aliases.properties"));
        assertEquals("something", aliases.getProperty("system.created"));

        // test the properties file dne
        warnings = (Integer) updatePropertyMethod.invoke(update, "project.prop=newvalue|oldvalue", configDir);
        assertEquals(1, warnings);
        Properties project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.properties"), false, true);
        assertNull(project);

        // test the properties file dne but is created
        warnings = (Integer) updatePropertyMethod.invoke(update, "project.prop=newvalue|", configDir);
        assertEquals(0, warnings);
        project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.properties"));
        assertEquals("newvalue", project.getProperty("prop"));

        // test scoped properties file, creation
        warnings = (Integer) updatePropertyMethod.invoke(update, "project#test.testprop=newvalue|", configDir);
        assertEquals(0, warnings);
        project = PropertiesFileUtil.load(FileUtil.pathFromParts(configDir.getPath(), "project.test.properties"));
        assertEquals("newvalue", project.getProperty("testprop"));
    }

    @After
    public void teardown() {
        File configDir = new File("src/test/resources/update-config");
        String aliasesFile = FileUtil.pathFromParts(configDir.getPath(), "aliases.properties");
        Properties aliases = PropertiesFileUtil.load(aliasesFile);
        aliases.clear();
        aliases.put("update", "ply-update-1.0.jar");
        aliases.put("user.modified", "something-else");
        PropertiesFileUtil.store(aliases, aliasesFile);

        FileUtil.delete(FileUtil.fromParts(configDir.getPath(), "project.properties"));
        FileUtil.delete(FileUtil.fromParts(configDir.getPath(), "project.test.properties"));

        configDir = new File("src/test/resources/update-ply-home");
        for (File file : configDir.listFiles()) {
            if ("holder.txt".equals(file.getName())) {
                continue;
            }
            FileUtil.delete(file);
        }
    }
    
}
