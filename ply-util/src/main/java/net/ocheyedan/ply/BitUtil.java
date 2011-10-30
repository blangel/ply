package net.ocheyedan.ply;

/**
 * User: blangel
 * Date: 10/3/11
 * Time: 9:40 PM
 *
 * Utility class to deal with bits and bytes within the java language.
 */
public final class BitUtil {

    public static String toHexString(byte[] array) {
        if (array == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(array.length * 2);
        for (byte byt : array) {
            int v = byt & 0xff;
            if (v < 16) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(v));
        }
        return sb.toString().toUpperCase();
    }

    private BitUtil() { }

}
