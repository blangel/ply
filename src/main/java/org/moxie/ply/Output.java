package org.moxie.ply;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 9/3/11
 * Time: 5:06 PM
 *
 * Defines the output mechanism within the {@literal Ply} application.
 * Support for colored VT-100 terminal output is controlled by the property {@literal color} within the
 * default context, {@literal ply}.
 */
public final class Output {

    /**
     * A regex {@link Pattern} paired with its corresponding output string.
     */
    private static final class TermCode {
        private final Pattern pattern;
        private final String output;
        private TermCode(Pattern pattern, String output) {
            this.pattern = pattern;
            this.output = output;
        }
    }

    /**
     * A mapping of easily identifiable words to a {@link TermCode} object for colored output.
     */
    private static final Map<String, TermCode> TERM_CODES = new HashMap<String, TermCode>();
    static {
        boolean withinTerminal = (System.getenv("TERM") != null);
        // first place color values (in case call to Config tries to print, at least have something in
        // TERM_CODES with which to strip messages.
        TERM_CODES.put("ply", new TermCode(Pattern.compile("\\^ply\\^"), withinTerminal ? "[\u001b[0;33mply\u001b[0m]" : "[ply]"));
        TERM_CODES.put("error", new TermCode(Pattern.compile("\\^error\\^"), withinTerminal ? "[\u001b[1;31merr!\u001b[0m]" : "[err!]"));
        TERM_CODES.put("warn", new TermCode(Pattern.compile("\\^warn\\^"), withinTerminal ? "[\u001b[1;33mwarn\u001b[0m]" : "[warn]"));
        TERM_CODES.put("info", new TermCode(Pattern.compile("\\^info\\^"), withinTerminal ? "[\u001b[1;34minfo\u001b[0m]" : "[info]"));
        TERM_CODES.put("reset", new TermCode(Pattern.compile("\\^r\\^"), withinTerminal ? "\u001b[0m" : ""));
        TERM_CODES.put("bold", new TermCode(Pattern.compile("\\^b\\^"), withinTerminal ? "\u001b[1m" : ""));
        TERM_CODES.put("normal", new TermCode(Pattern.compile("\\^n\\^"), withinTerminal ? "\u001b[2m" : ""));
        TERM_CODES.put("inverse", new TermCode(Pattern.compile("\\^i\\^"), withinTerminal ? "\u001b[7m" : ""));
        TERM_CODES.put("black", new TermCode(Pattern.compile("\\^black\\^"), withinTerminal ? "\u001b[1;30m" : ""));
        TERM_CODES.put("red", new TermCode(Pattern.compile("\\^red\\^"), withinTerminal ? "\u001b[1;31m" : ""));
        TERM_CODES.put("green", new TermCode(Pattern.compile("\\^green\\^"), withinTerminal ? "\u001b[1;32m" : ""));
        TERM_CODES.put("yellow", new TermCode(Pattern.compile("\\^yellow\\^"), withinTerminal ? "\u001b[1;33m" : ""));
        TERM_CODES.put("blue", new TermCode(Pattern.compile("\\^blue\\^"), withinTerminal ? "\u001b[1;34m" : ""));
        TERM_CODES.put("magenta", new TermCode(Pattern.compile("\\^magenta\\^"), withinTerminal ? "\u001b[1;35m" : ""));
        TERM_CODES.put("cyan", new TermCode(Pattern.compile("\\^cyan\\^"), withinTerminal ? "\u001b[1;36m" : ""));
        TERM_CODES.put("white", new TermCode(Pattern.compile("\\^white\\^"), withinTerminal ? "\u001b[1;37m" : ""));
    }

    private static final AtomicReference<Boolean> warnLevel = new AtomicReference<Boolean>(true);
    private static final AtomicReference<Boolean> infoLevel = new AtomicReference<Boolean>(true);

    /**
     * Remaps the {@link #TERM_CODES} appropriately if the {@literal color} property is false.
     * Also figures out what log levels are available.
     * Requires property resolution and so is post-static initialization.
     */
    static void init() {
        String colorProp = Config.get("color");
        boolean color = (colorProp == null || !"false".equals(colorProp));
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
        String logLevelsProp = Config.get("log.levels");
        if (!logLevelsProp.contains("warn")) {
            warnLevel.set(false);
        }
        if (!logLevelsProp.contains("info")) {
            infoLevel.set(false);
        }
    }

    public static void print(String message, Object ... args) {
        String formatted = String.format(message, args);
        // TODO - fix!  this case fails: ^cyan^warn^r^ if ^warn^ is evaluated first...really meant for ^cyan^ and ^r^
        // TODO - to be resolved
        for (String key : TERM_CODES.keySet()) {
            TermCode termCode = TERM_CODES.get(key);
            Matcher matcher = termCode.pattern.matcher(formatted);
            if (matcher.find()) {
                if (("warn".equals(key) && !warnLevel.get()) || ("info".equals(key) && !infoLevel.get())) {
                    // this is a log statement for a disabled log-level, skip.
                    return;
                }
                formatted = matcher.replaceAll(termCode.output);
            }
        }
        System.out.println(formatted);
    }

    public static void print(Throwable t) {
        Output.print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }
}
