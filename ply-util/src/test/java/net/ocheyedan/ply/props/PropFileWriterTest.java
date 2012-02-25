package net.ocheyedan.ply.props;

import org.junit.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 2/17/12
 * Time: 9:46 PM
 */
public class PropFileWriterTest {

    @Test
    public void store() throws IOException {
        PropFileWriter writer = PropFileWriter.Default;

        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);

        PropFile propFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        propFile.add("key_1", "value_1");
        propFile.add("key_2", "unfilteredValue", " comment line 1\n comment line 2");
        propFile.add("key_3", "value_3 with spaces", "more comments and more and more");
        writer.store(bufferedWriter, propFile);
        bufferedWriter.flush();
        stringWriter.flush();
        String propertiesFile = stringWriter.toString();
        assertEquals("key_1=value_1\n" +
                "# comment line 1\n" +
                "# comment line 2\n" +
                "key_2=unfilteredValue\n" +
                "#more comments and more and more\n" +
                "key_3=value_3 with spaces\n", propertiesFile);


        stringWriter = new StringWriter();
        bufferedWriter = new BufferedWriter(stringWriter);

        propFile = new PropFile(Context.named("test"), PropFile.Loc.Local);
        propFile.add("key=1", "value=1");
        propFile.add("key=2", "unfilteredValue", " comment line = 1\n comment line 2");
        propFile.add("key=3", "value=3 with spaces", "more comments and more and more");
        writer.store(bufferedWriter, propFile);
        bufferedWriter.flush();
        stringWriter.flush();
        propertiesFile = stringWriter.toString();
        assertEquals("key\\=1=value\\=1\n" +
                "# comment line = 1\n" +
                "# comment line 2\n" +
                "key\\=2=unfilteredValue\n" +
                "#more comments and more and more\n" +
                "key\\=3=value\\=3 with spaces\n", propertiesFile);

    }

}
