package net.ocheyedan.ply.jna.lib;

import com.sun.jna.Library;

/**
 * User: blangel
 * Date: 11/8/11
 * Time: 7:29 PM
 *
 * Facade to native c-library for linux.
 */
public interface CUnixLibrary extends Library {

    int symlink(String targetPath, String linkPath);

}
