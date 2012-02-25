package net.ocheyedan.ply.props;

import org.junit.Test;

import java.io.*;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 2/18/12
 * Time: 8:04 AM
 */
public class PropFilesTest {

    @Test
    public void store() throws IOException {

        // test null case
        assertFalse(PropFiles.store(null, null));
        assertFalse(PropFiles.store(null, null, false));
        assertFalse(PropFiles.store(null, null, null, false));

        PropFile empty = new PropFile(Context.named("test"), PropFile.Loc.Local);
        // test fnf and create false
        assertFalse(PropFiles.store(empty, null));
        assertFalse(PropFiles.store(empty, "not a file", false));

        File tmp = File.createTempFile("test", "store");
        PropFile props = new PropFile(Context.named("test"), PropFile.Loc.Local);
        long now = System.currentTimeMillis();
        props.add("test", String.valueOf(now));
        assertTrue(PropFiles.store(props, tmp.getPath(), true));

        PropFile loaded = new PropFile(Context.named("test"), PropFile.Loc.Local);
        PropFileReader.Default.load(new BufferedReader(new FileReader(tmp)), loaded);

        assertEquals(now, (long) Long.valueOf(loaded.get("test").value()));
    }

    @Test
    public void load() throws IOException {

        try {
            PropFiles.load(null, null);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            PropFiles.load(null, null, false);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            PropFiles.load(null, null, false, false);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            PropFiles.load(null, null, null, false, false);
            fail("Expecting a NullPointerException.");
        } catch (NullPointerException npe) {
            // expected
        }

        File tmp = File.createTempFile("test", "store");
        PropFile props = new PropFile(Context.named("test"), PropFile.Loc.Local);
        long now = System.currentTimeMillis();
        props.add("test", String.valueOf(now));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tmp));
        PropFileWriter.Default.store(writer, props);
        writer.close();

        PropFile loaded = new PropFile(Context.named("test"), PropFile.Loc.Local);
        assertTrue(PropFiles.load(tmp.getPath(), loaded, false, false));
        assertEquals(now, (long) Long.valueOf(loaded.get("test").value()));

        loaded = new PropFile(Context.named("test"), PropFile.Loc.Local);
        assertFalse(PropFiles.load("not a file", loaded, false, false));
    }
    
}
