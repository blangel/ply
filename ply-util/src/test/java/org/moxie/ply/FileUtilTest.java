package org.moxie.ply;

import org.junit.Test;

import java.io.*;
import java.net.URL;
import java.nio.CharBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 8:56 PM
 */
public class FileUtilTest {

    @Test
    public void copy() throws IOException {
        File fileOne = File.createTempFile("one", ".txt");
        File fileTwo = File.createTempFile("two", ".txt");

        String content = "testing" + System.currentTimeMillis();

        FileWriter writer = new FileWriter(fileOne);
        writer.write(content);
        writer.close();

        FileUtil.copy(fileOne, fileTwo);

        FileInputStream stream = new FileInputStream(fileTwo);
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        String copied = Charset.defaultCharset().decode(bb).toString();
        stream.close();

        assertEquals(content, copied);
    }

    @Test
    public void copyUrl() throws IOException {
        File fileOne = File.createTempFile("one", ".txt");
        File fileTwo = File.createTempFile("two", ".txt");

        String content = "testing" + System.currentTimeMillis();

        FileWriter writer = new FileWriter(fileOne);
        writer.write(content);
        writer.close();

        FileUtil.copy(new URL("file://" + fileOne.getCanonicalPath()), fileTwo);

        FileInputStream stream = new FileInputStream(fileTwo);
        FileChannel fc = stream.getChannel();
        MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        String copied = Charset.defaultCharset().decode(bb).toString();
        stream.close();

        assertEquals(content, copied);
    }

}
