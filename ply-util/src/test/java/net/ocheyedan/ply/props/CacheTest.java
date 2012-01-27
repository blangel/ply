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
        String key = Cache.getKey(testDir, false);
        assertEquals("/tmp-false", key);

        testDir = new File("/tmp/another");
        key = Cache.getKey(testDir, false);
        assertEquals("/tmp/another-false", key);
    }

    @Test public void get() {
        File testDir = new File("/tmp");
        Collection<Prop.All> props = Cache.get(testDir, false);
        assertNull(props);
        Collection<Prop.All> placed = new HashSet<Prop.All>(1);
        Cache.put(testDir, false, placed);
        assertSame(placed, Cache.get(testDir, false));
        assertNotNull(Cache.get(testDir, false));
        assertEquals(0, Cache.get(testDir, false).size());
    }

    @Test public void put() {
        File testDir = new File("/tmp");
        assertNull(Cache.get(testDir, true));
        Collection<Prop.All> placed = new HashSet<Prop.All>(1);
        Cache.put(testDir, true, placed);
        assertSame(placed, Cache.get(testDir, true));
    }

}
