package net.ocheyedan.ply;

/**
 * User: blangel
 * Date: 1/13/12
 * Time: 4:54 PM
 *
 * Provides access to package-protected methods from {@link Output}.
 */
public class OutputExt {

    public static void printFromExec(String message, Object ... args) { // TODO - rename
        Output.printFromExec(message, args);
    }

    public static String resolve(String message, Object[] args) {
        return Output.resolve(message, args);
    }

    public static void init() {
        Output.init();
    }

}
