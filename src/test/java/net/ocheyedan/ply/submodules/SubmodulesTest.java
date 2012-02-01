package net.ocheyedan.ply.submodules;

import org.junit.Test;

import java.io.File;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * User: blangel
 * Date: 1/30/12
 * Time: 9:43 PM
 */
public class SubmodulesTest {

    @Test
    public void getSubmodules() {
        File configDir = new File("./src/test/resources/dot-ply/config");
        List<Submodule> submodules = Submodules.getSubmodules(configDir);
        assertEquals(3, submodules.size());
        assertEquals("child-1", submodules.get(0).name);
        assertEquals("ply:ply-unit-test-child-1:1.0", submodules.get(0).dependencyName);
        assertEquals("child-2", submodules.get(1).name);
        assertEquals("ply:ply-unit-test-child-2:1.0", submodules.get(1).dependencyName);
        assertEquals("child-3", submodules.get(2).name);
        assertEquals("ply:ply-unit-test-child-3:1.0", submodules.get(2).dependencyName);

    }

}
