package net.ocheyedan.ply.jna;

import com.sun.jna.Native;
import net.ocheyedan.ply.jna.lib.CUnixLibrary;

/**
 * User: blangel
 * Date: 11/8/11
 * Time: 7:36 PM
 *
 * Provides access to the {@literal net.ocheyedan.ply.jna.lib} interfaces.
 */
public final class JnaAccessor {

    private static final CUnixLibrary cUnixLibrary;

    static {
        if (JnaUtil.isJnaPresent() && JnaUtil.getOperatingSystem().isUnix()) {
            cUnixLibrary = (CUnixLibrary) Native.loadLibrary("c", CUnixLibrary.class);
        } else {
            cUnixLibrary = null;
        }
    }

    public static CUnixLibrary getCUnixLibrary() {
        return cUnixLibrary;
    }

    private JnaAccessor() { }

}
