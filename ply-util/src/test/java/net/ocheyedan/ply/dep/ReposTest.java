package net.ocheyedan.ply.dep;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.props.Scope;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * User: blangel
 * Date: 6/29/16
 * Time: 10:12 PM
 */
public class ReposTest {

    @Test
    public void createChecksumFile() throws Exception {
        Method createChecksumFileMethod = Repos.class.getDeclaredMethod("createChecksumFile", Scope.class, String.class);
        createChecksumFileMethod.setAccessible(true);

        // null value
        String result = (String) createChecksumFileMethod.invoke(null, Scope.Default, null);
        assertNull(result);

        // empty value
        result = (String) createChecksumFileMethod.invoke(null, Scope.Default, null);
        assertNull(result);

        // no file separator
        result = (String) createChecksumFileMethod.invoke(null, Scope.Default, "foo");
        assertNull(result);

        // valid, default scope
        result = (String) createChecksumFileMethod.invoke(null, Scope.Default, FileUtil.pathFromParts("foo", "bar"));
        assertEquals("foo/checksum.properties", result);

    }

}
