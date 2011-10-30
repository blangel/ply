package net.ocheyedan.ply.script.print;

import net.ocheyedan.ply.Output;

/**
 * User: blangel
 * Date: 10/29/11
 * Time: 2:50 PM
 *
 * Prefixes all print statements with {@link PrivilegedPrintStream#PRIVILEGED_PREFIX}
 */
public final class PrivilegedOutput {

    public static void print(String message, Object ... args) {
        Output.print(PrivilegedPrintStream.PRIVILEGED_PREFIX + message, args);
    }

    public static void print(Throwable t) {
        print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }

}
