package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;
import org.junit.Test;

import java.io.*;
import java.util.Iterator;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

/**
 * User: blangel
 * Date: 2/17/12
 * Time: 8:30 PM
 */
public class PropFileReaderTest {

    @Test
    public void load() throws IOException {
        PropFileReader reader = PropFileReader.Default;
        try {
            reader.load(null, null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            reader.load(null, new PropFile(Context.named("test"), PropFile.Loc.Local));
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        try {
            reader.load(new BufferedReader(new CharArrayReader(new char[0])), null);
            fail("Expecting a NullPointerException");
        } catch (NullPointerException npe) {
            // expected
        }
        
        File base;
        if (FileUtil.getCanonicalPath(new File("./")).contains("ply-util")) {
            base = new File("./");
        } else {
            base = new File("ply-util/");
        }

        File mockInvalid1 = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-files/mock-invalid-1.properties");
        PropFile propFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        try {
            reader.load(new BufferedReader(new InputStreamReader(new FileInputStream(mockInvalid1))), propFile);
            fail("Expecting an Invalid exception");
        } catch (PropFileReader.Invalid pfri) {
            assertEquals("Keys must be non-empty.", pfri.getMessage());
        }
        File mockInvalid2 = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-files/mock-invalid-2.properties");
        try {
            reader.load(new BufferedReader(new InputStreamReader(new FileInputStream(mockInvalid2))), propFile);
            fail("Expecting an Invalid exception");
        } catch (PropFileReader.Invalid pfri) {
            assertEquals("Keys must be non-empty.", pfri.getMessage());
        }
        File mockInvalid3 = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-files/mock-invalid-3.properties");
        try {
            reader.load(new BufferedReader(new InputStreamReader(new FileInputStream(mockInvalid3))), propFile);
            fail("Expecting an Invalid exception");
        } catch (PropFileReader.Invalid pfri) {
            assertEquals("Keys must be unique.", pfri.getMessage());
        }
        File mockInvalid4 = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-files/mock-invalid-4.properties");
        try {
            reader.load(new BufferedReader(new InputStreamReader(new FileInputStream(mockInvalid4))), propFile);
            fail("Expecting an Invalid exception");
        } catch (PropFileReader.Invalid pfri) {
            assertEquals("Properties may only have one key.", pfri.getMessage());
        }
        File mock1 = FileUtil.fromParts(base.getPath(), "src/test/resources/mock-prop-files/mock-1.properties");
        propFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        reader.load(new BufferedReader(new InputStreamReader(new FileInputStream(mock1))), propFile);
        Iterator<PropFile.Prop> iterator = propFile.props().iterator();
        PropFile.Prop prop = iterator.next();
        assertEquals("key_1", prop.name);
        assertEquals("value", prop.value());
        assertEquals(" comments for key_1", prop.comments());

        prop = iterator.next();
        assertEquals("key_2", prop.name);
        assertEquals("value_2", prop.value());
        assertEquals(" comments for key_2\n which go onto another line", prop.comments());

        prop = iterator.next();
        assertEquals("key_3", prop.name);
        assertEquals("value_3:with:colon", prop.value());
        assertEquals("", prop.comments());

        prop = iterator.next();
        assertEquals("key_4:with:colon", prop.name);
        assertEquals("value_5_and_6", prop.value());
        assertEquals(" comments for key_4\n again on another line and note the escaped key", prop.comments());

        prop = iterator.next();
        assertEquals("key_7", prop.name);
        assertEquals("more and more and more", prop.value());
        assertEquals(" notice the blank space between, both before and after this comment", prop.comments());
    }

}
