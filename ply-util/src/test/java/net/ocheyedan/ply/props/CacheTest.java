package net.ocheyedan.ply.props;

import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import static junit.framework.Assert.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 8:39 AM
 */
public class CacheTest {

    @Test public void getKey() {
        File testDir = new File("/tmp");
        String key = Cache.getKey(testDir);
        assertEquals("/tmp", key);

        testDir = new File("/tmp/another");
        key = Cache.getKey(testDir);
        assertEquals("/tmp/another", key);
    }

    @Test public void get() {
        File testDir = new File("/tmp");
        Collection<Prop.All> props = Cache.get(testDir);
        assertNull(props);
        Collection<Prop.All> placed = new HashSet<Prop.All>(1);
        Cache.put(testDir, placed);
        assertSame(placed, Cache.get(testDir));
        assertNotNull(Cache.get(testDir));
        assertEquals(0, Cache.get(testDir).size());
    }

    @Test public void put() {
        File testDir = new File("/tmp");
        Collection<Prop.All> props = Cache.get(testDir);
        assertNotNull(Cache.get(testDir));
        assertEquals(0, Cache.get(testDir).size());
        Collection<Prop.All> placed = new HashSet<Prop.All>(1);
        Cache.put(testDir, placed);
        assertSame(placed, Cache.get(testDir));
    }

}
