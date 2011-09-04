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

    private static final class TermCode {
        private final Pattern pattern;
        private final String output;
        private TermCode(Pattern pattern, String output) {
            this.pattern = pattern;
            this.output = output;
        }
    }

    private static final Map<String, TermCode> TERM_CODES = new HashMap<String, TermCode>();
    static {
        // first place color values (in case call to Config tries to print, at least have something in
        // TERM_CODES with which to strip messages.
        TERM_CODES.put("ply", new TermCode(Pattern.compile("\\^ply\\^"), "[\u001b[1m\u001b[1;33mply\u001b[0m]"));
        TERM_CODES.put("error", new TermCode(Pattern.compile("\\^error\\^"), "[\u001b[1m\u001b[1;31merr!\u001b[0m]"));
        TERM_CODES.put("warn", new TermCode(Pattern.compile("\\^warn\\^"), "[\u001b[1m\u001b[1;33mwarn\u001b[0m]"));
        TERM_CODES.put("info", new TermCode(Pattern.compile("\\^info\\^"), "[\u001b[1m\u001b[1;36minfo\u001b[0m]"));
        TERM_CODES.put("reset", new TermCode(Pattern.compile("\\^r\\^"), "\u001b[0m"));
        TERM_CODES.put("bold", new TermCode(Pattern.compile("\\^b\\^"), "\u001b[1m"));
        TERM_CODES.put("normal", new TermCode(Pattern.compile("\\^n\\^"), "\u001b[2m"));
        TERM_CODES.put("inverse", new TermCode(Pattern.compile("\\^i\\^"), "\u001b[7m"));
        TERM_CODES.put("black", new TermCode(Pattern.compile("\\^black\\^"), "\u001b[1;30m"));
        TERM_CODES.put("red", new TermCode(Pattern.compile("\\^red\\^"), "\u001b[1;31m"));
        TERM_CODES.put("green", new TermCode(Pattern.compile("\\^green\\^"), "\u001b[1;32m"));
        TERM_CODES.put("yellow", new TermCode(Pattern.compile("\\^yellow\\^"), "\u001b[1;33m"));
        TERM_CODES.put("blue", new TermCode(Pattern.compile("\\^blue\\^"), "\u001b[1;34m"));
        TERM_CODES.put("magenta", new TermCode(Pattern.compile("\\^magenta\\^"), "\u001b[1;35m"));
        TERM_CODES.put("cyan", new TermCode(Pattern.compile("\\^cyan\\^"), "\u001b[1;36m"));
        TERM_CODES.put("white", new TermCode(Pattern.compile("\\^white\\^"), "\u001b[1;37m"));
        Config.Prop colorProp = Config.get("color");
        boolean color = (colorProp == null || !"false".equals(colorProp.value));
        if (!color) {
            TERM_CODES.put("ply", new TermCode(TERM_CODES.get("ply").pattern, "[ply]"));
            TERM_CODES.put("error", new TermCode(TERM_CODES.get("error").pattern, "[err!]"));
            TERM_CODES.put("warn", new TermCode(TERM_CODES.get("warn").pattern, "[warn]"));
            TERM_CODES.put("info", new TermCode(TERM_CODES.get("info").pattern, "[info]"));
            TERM_CODES.put("reset", new TermCode(TERM_CODES.get("reset").pattern, ""));
            TERM_CODES.put("bold", new TermCode(TERM_CODES.get("bold").pattern, ""));
            TERM_CODES.put("normal", new TermCode(TERM_CODES.get("normal").pattern, ""));
            TERM_CODES.put("inverse", new TermCode(TERM_CODES.get("inverse").pattern, ""));
            TERM_CODES.put("black", new TermCode(TERM_CODES.get("black").pattern, ""));
            TERM_CODES.put("red", new TermCode(TERM_CODES.get("red").pattern, ""));
            TERM_CODES.put("green", new TermCode(TERM_CODES.get("green").pattern, ""));
            TERM_CODES.put("yellow", new TermCode(TERM_CODES.get("yellow").pattern, ""));
            TERM_CODES.put("blue", new TermCode(TERM_CODES.get("blue").pattern, ""));
            TERM_CODES.put("magenta", new TermCode(TERM_CODES.get("magenta").pattern, ""));
            TERM_CODES.put("cyan", new TermCode(TERM_CODES.get("cyan").pattern, ""));
            TERM_CODES.put("white", new TermCode(TERM_CODES.get("white").pattern, ""));
        }

    }

    public static void print(String message, Object ... args) {
        for (String key : TERM_CODES.keySet()) {
            TermCode termCode = TERM_CODES.get(key);
            message = termCode.pattern.matcher(message).replaceAll(termCode.output);
        }
        System.out.printf(message + "\n", args);
    }

    public static void print(Throwable t) {
        Output.print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }



}
