package net.ocheyedan.ply;

import org.junit.Test;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import static junit.framework.Assert.*;

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

    @Test
    public void copyDir() throws IOException {
        long stamp = System.currentTimeMillis();
        File from = new File("/tmp/" + stamp);
        File to = new File("/tmp/copyOf" + stamp);

        final File fromTmpDir = new File(from.getPath() + File.separator + "recur");
        fromTmpDir.mkdirs();

        File tmp = File.createTempFile("one", ".txt", from);
        String content = "testing" + System.currentTimeMillis();
        FileWriter writer = new FileWriter(tmp);
        writer.write(content);
        writer.close();
        File tmp2 = File.createTempFile("two", ".txt", fromTmpDir);
        writer = new FileWriter(tmp2);
        writer.write(content + "two");
        writer.close();

        FileUtil.copyDir(from, to);

        to = new File("/tmp/copyOf" + stamp);

        File[] toFiles = to.listFiles();
        assertEquals(2, toFiles.length);
        for (File subFile : toFiles) {
            if (subFile.isDirectory()) {
                File[] subToFiles = subFile.listFiles();
                assertEquals(1, subToFiles.length);
                FileInputStream stream = new FileInputStream(subToFiles[0]);
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                String copied = Charset.defaultCharset().decode(bb).toString();
                stream.close();
                assertEquals(content + "two", copied);
            } else {
                FileInputStream stream = new FileInputStream(subFile);
                FileChannel fc = stream.getChannel();
                MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
                String copied = Charset.defaultCharset().decode(bb).toString();
                stream.close();
                assertEquals(content, copied);
            }
        }

        to = new File("/tmp/copyOf" + stamp + stamp);

        FileUtil.copyDir(from, to, new FilenameFilter() {
            @Override public boolean accept(File dir, String name) {
                return (name.equals(fromTmpDir.getName()));
            }
        });

        to = new File("/tmp/copyOf" + stamp + stamp);

        toFiles = to.listFiles();
        assertEquals(1, toFiles.length);

        for (File subFile : toFiles) {
            FileInputStream stream = new FileInputStream(subFile);
            FileChannel fc = stream.getChannel();
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            String copied = Charset.defaultCharset().decode(bb).toString();
            stream.close();
            assertEquals(content, copied);
        }
    }

    @Test
    public void pathFromParts() {
        assertNull(FileUtil.pathFromParts());
        assertNull(FileUtil.pathFromParts((String[]) null));

        assertEquals("", FileUtil.pathFromParts(""));
        assertEquals("test", FileUtil.pathFromParts("test"));
        assertEquals("null", FileUtil.pathFromParts((String) null));

        assertEquals("test" + File.separator + "again", FileUtil.pathFromParts("test", "again"));
        assertEquals("test" + File.separator + "again" + File.separator + "more", FileUtil.pathFromParts("test", "again", "more"));

        assertEquals("test" + File.separator + "again", FileUtil.pathFromParts("test" + File.separator, "again"));
        assertEquals("test" + File.separator + "again" + File.separator + "more" + File.separator,
                        FileUtil.pathFromParts("test" + File.separator, "again" + File.separator, "more" + File.separator));
    }

    @Test
    public void fromParts() {

        assertNull(FileUtil.fromParts());
        assertNull(FileUtil.fromParts((String[]) null));

        assertEquals("null", FileUtil.fromParts((String) null).getPath());
        assertEquals("", FileUtil.fromParts("").getPath());
        assertEquals("test", FileUtil.fromParts("test").getPath());

        assertEquals("test" + File.separator + "again", FileUtil.fromParts("test", "again").getPath());
        assertEquals("test" + File.separator + "again" + File.separator + "more", FileUtil.fromParts("test", "again", "more").getPath());

        assertEquals("test" + File.separator + "again", FileUtil.fromParts("test" + File.separator, "again").getPath());
        assertEquals("test" + File.separator + "again" + File.separator + "more", FileUtil.fromParts("test" + File.separator,
                                                                                                     "again" + File.separator,
                                                                                                     "more" + File.separator).getPath());

    }

    @Test
    public void download() throws IOException {
        Map<String, String> empty = Collections.emptyMap();
        assertFalse(FileUtil.download(null, empty, null, null, null, true));
        URL url = URI.create("http://dne.com/dne/dne/dne.html").toURL();
        assertFalse(FileUtil.download(url, empty, null, null, null, true));
        File tmp = File.createTempFile("ply-test", ".tmp");
        url = URI.create("https://raw.github.com/blangel/ply/master/README.md").toURL();
        assertTrue(FileUtil.download(url, empty, tmp, "ply-test.tmp", "tmp location", true));
    }

    @Test
    public void getLocalPath() throws IOException {
        Map<String, String> empty = Collections.emptyMap();
        assertNull(FileUtil.getLocalPath(null, empty, null, null));

        File tmp = File.createTempFile("ply-test", ".tmp");
        URL url = tmp.toURI().toURL();
        String path = FileUtil.getLocalPath(url, empty, "ply-test", "tmp location");
        assertNotNull(path);
        assertEquals(tmp.getPath(), path);
        String expectedPath = "/Users/ply/test.tmp";
        url = URI.create("file:" + expectedPath).toURL();
        path = FileUtil.getLocalPath(url, empty, "ply-test", "tmp location");
        assertNotNull(path);
        assertEquals(expectedPath, path);

        url = URI.create("http://dne.com/dne/dne/dne.html").toURL();
        assertNull(FileUtil.getLocalPath(url, empty, "dne", "tmp location"));
        url = URI.create("https://raw.github.com/blangel/ply/master/README.md").toURL();
        path = FileUtil.getLocalPath(url, empty, "readme", "tmp location");
        assertNotNull(path);
        assertTrue(path.contains("ply-"));
        assertTrue(path.endsWith(".tmp"));
    }

    @Test
    public void getSha1Hash() throws IOException {
        File file = File.createTempFile("foo", "bar");
        String hash = FileUtil.getSha1Hash(file);
        assertEquals("DA39A3EE5E6B4B0D3255BFEF95601890AFD80709", hash);
    }


}
