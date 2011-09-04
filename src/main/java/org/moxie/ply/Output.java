package org.moxie.ply;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 5:06 PM
 *
 * Defines the output mechanism within the {@literal Ply} application.
 * Support for colored VT-100 terminal output is controlled by the property {@literal color}.
 */
public final class Output {

    private static final Map<Pattern, String> TERM_CODES = new HashMap<Pattern, String>();
    static {
        // first place color values (in case call to Config tries to print, at least have something in
        // TERM_CODES with which to strip messages.
        TERM_CODES.put(Pattern.compile("\\^ply\\^"), "[\u001b[1m\u001b[1;33mply\u001b[0m]");
        TERM_CODES.put(Pattern.compile("\\^error\\^"), "[\u001b[1m\u001b[1;31merr!\u001b[0m]");
        TERM_CODES.put(Pattern.compile("\\^warn\\^"), "[\u001b[1m\u001b[1;33mwarn\u001b[0m]");
        TERM_CODES.put(Pattern.compile("\\^info\\^"), "[\u001b[1m\u001b[1;36minfo\u001b[0m]");
        TERM_CODES.put(Pattern.compile("\\^r\\^"), "\u001b[0m");
        TERM_CODES.put(Pattern.compile("\\^b\\^"), "\u001b[1m");
        TERM_CODES.put(Pattern.compile("\\^n\\^"), "\u001b[2m");
        TERM_CODES.put(Pattern.compile("\\^i\\^"), "\u001b[7m");
        TERM_CODES.put(Pattern.compile("\\^black\\^"), "\u001b[1;30m");
        TERM_CODES.put(Pattern.compile("\\^red\\^"), "\u001b[1;31m");
        TERM_CODES.put(Pattern.compile("\\^green\\^"), "\u001b[1;32m");
        TERM_CODES.put(Pattern.compile("\\^yellow\\^"), "\u001b[1;33m");
        TERM_CODES.put(Pattern.compile("\\^blue\\^"), "\u001b[1;34m");
        TERM_CODES.put(Pattern.compile("\\^magenta\\^"), "\u001b[1;35m");
        TERM_CODES.put(Pattern.compile("\\^cyan\\^"), "\u001b[1;36m");
        TERM_CODES.put(Pattern.compile("\\^white\\^"), "\u001b[1;37m");
        Config.Prop colorProp = Config.get("color");
        boolean color = (colorProp == null || !"false".equals(colorProp.value));
        if (!color) {
            TERM_CODES.put(Pattern.compile("\\^ply\\^"), "[ply]");
            TERM_CODES.put(Pattern.compile("\\^error\\^"), "[err!]");
            TERM_CODES.put(Pattern.compile("\\^warn\\^"), "[warn]");
            TERM_CODES.put(Pattern.compile("\\^info\\^"), "[info]");
            TERM_CODES.put(Pattern.compile("\\^r\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^b\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^n\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^i\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^black\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^red\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^green\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^yellow\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^blue\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^magenta\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^cyan\\^"), "");
            TERM_CODES.put(Pattern.compile("\\^white\\^"), "");
        }

    }

    public static void print(String message, Object ... args) {
        for (Pattern pattern : TERM_CODES.keySet()) {
            message = pattern.matcher(message).replaceAll(TERM_CODES.get(pattern));
        }
        System.out.printf(message + "\n", args);
    }

    public static void print(Throwable t) {
        Output.print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }



}
