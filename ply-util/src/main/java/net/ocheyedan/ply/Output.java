package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/2/11
 * Time: 9:18 PM
 *
 * Defines the output mechanism within the {@literal Ply} application.
 * Support for colored VT-100 terminal output is controlled by the property {@literal color} within the
 * default context, {@literal ply}.
 */
public final class Output {

    /**
     * A regex {@link java.util.regex.Pattern} paired with its corresponding output string.
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
     * Configurable log level variables.
     */
    private static final AtomicReference<Boolean> warnLevel = new AtomicReference<Boolean>(true);
    private static final AtomicReference<Boolean> infoLevel = new AtomicReference<Boolean>(true);
    private static final AtomicReference<Boolean> dbugLevel = new AtomicReference<Boolean>(true);
    private static final boolean unicodeSupport;

    /**
     * A mapping of easily identifiable words to a {@link TermCode} object for colored output.
     */
    private static final Map<String, TermCode> TERM_CODES = new HashMap<String, TermCode>();
    static {
        String terminal = System.getenv("TERM");
        boolean withinTerminal = (terminal != null);
        // TODO - what are the range of terminal values and what looks best for each?
        String terminalBold = ("xterm".equals(terminal) ? "1" : "0");
        Prop colorProp = Props.get("color");
        boolean colorDisabled = ((colorProp != null) && "false".equalsIgnoreCase(colorProp.value));
        boolean useColor = withinTerminal && !colorDisabled;
        // first place color values (in case call to Config tries to print, at least have something in
        // TERM_CODES with which to strip messages.
        TERM_CODES.put("ply", new TermCode(Pattern.compile("\\^ply\\^"), useColor ? "[\u001b[0;33mply\u001b[0m]" : "[ply]"));
        TERM_CODES.put("error", new TermCode(Pattern.compile("\\^error\\^"), useColor ? "[\u001b[1;31merr!\u001b[0m]" : "[err!]"));
        TERM_CODES.put("warn", new TermCode(Pattern.compile("\\^warn\\^"), useColor ? "[\u001b[1;33mwarn\u001b[0m]" : "[warn]"));
        TERM_CODES.put("info", new TermCode(Pattern.compile("\\^info\\^"), useColor ? "[\u001b[1;34minfo\u001b[0m]" : "[info]"));
        TERM_CODES.put("dbug", new TermCode(Pattern.compile("\\^dbug\\^"), useColor ? "[\u001b[1;30mdbug\u001b[0m]" : "[dbug]"));
        TERM_CODES.put("reset", new TermCode(Pattern.compile("\\^r\\^"), useColor ? "\u001b[0m" : ""));
        TERM_CODES.put("bold", new TermCode(Pattern.compile("\\^b\\^"), useColor ? "\u001b[1m" : ""));
        TERM_CODES.put("normal", new TermCode(Pattern.compile("\\^n\\^"), useColor ? "\u001b[2m" : ""));
        TERM_CODES.put("inverse", new TermCode(Pattern.compile("\\^i\\^"), useColor ? "\u001b[7m" : ""));
        TERM_CODES.put("black", new TermCode(Pattern.compile("\\^black\\^"), useColor ? "\u001b[" + terminalBold + ";30m" : ""));
        TERM_CODES.put("red", new TermCode(Pattern.compile("\\^red\\^"), useColor ? "\u001b[" + terminalBold + ";31m" : ""));
        TERM_CODES.put("green", new TermCode(Pattern.compile("\\^green\\^"), useColor ? "\u001b[" + terminalBold + ";32m" : ""));
        TERM_CODES.put("yellow", new TermCode(Pattern.compile("\\^yellow\\^"), useColor ? "\u001b[" + terminalBold + ";33m" : ""));
        TERM_CODES.put("blue", new TermCode(Pattern.compile("\\^blue\\^"), useColor ? "\u001b[" + terminalBold + ";34m" : ""));
        TERM_CODES.put("magenta", new TermCode(Pattern.compile("\\^magenta\\^"), useColor ? "\u001b[" + terminalBold + ";35m" : ""));
        TERM_CODES.put("cyan", new TermCode(Pattern.compile("\\^cyan\\^"), useColor ? "\u001b[" + terminalBold + ";36m" : ""));
        TERM_CODES.put("white", new TermCode(Pattern.compile("\\^white\\^"), useColor ? "\u001b[" + terminalBold + ";37m" : ""));
        if (Props.get("log.levels") != null) {
            init(Props.getValue("log.levels"));
        }
        Prop unicodeProp = Props.get("unicode");
        unicodeSupport = ((unicodeProp == null) || "true".equalsIgnoreCase(unicodeProp.value));
    }

    static void init(String logLevels) {
        if (!logLevels.contains("warn")) {
            warnLevel.set(false);
        }
        if (!logLevels.contains("info")) {
            infoLevel.set(false);
        }
        if (!logLevels.contains("debug") && !logLevels.contains("dbug")) {
            dbugLevel.set(false);
        }
    }

    public static void print(String message, Object ... args) {
        String formatted = resolve(message, args);
        if (formatted == null) {
            return;
        }
        System.out.println(formatted);
    }

    public static void printNoLine(String message, Object ... args) {
        String formatted = resolve(message, args);
        if (formatted == null) {
            return;
        }
        System.out.print(formatted);
    }

    static void printFromExec(String message, Object ... args) {
        String scriptArg = (String) args[1];
        boolean noLine = scriptArg.contains("^no_line^");
        boolean noPrefix = scriptArg.contains("^no_prefix^");
        if (noPrefix && noLine) {
            printNoLine("%s", scriptArg.replaceFirst("\\^no_line\\^", "").replaceFirst("\\^no_prefix\\^", ""));
        } else if (noPrefix) {
            print("%s", scriptArg.replaceFirst("\\^no_prefix\\^", ""));
        } else if (noLine) {
            printNoLine(message, args[0], scriptArg.replaceFirst("\\^no_line\\^", ""));
        } else {
            print(message, args);
        }
    }

    public static void print(Throwable t) {
        print("^error^ Message: ^i^^red^%s^r^", (t == null ? "" : t.getMessage()));
    }

    /**
     * @return true if unicode is supported as output
     */
    public static boolean isUnicode() {
        return unicodeSupport;
    }

    private static String resolve(String message, Object[] args) {
        String formatted = String.format(message, args);
        // TODO - fix!  this case fails: ^cyan^warn^r^ if ^warn^ is evaluated first...really meant for ^cyan^ and ^r^
        // TODO - to be resolved
        for (String key : TERM_CODES.keySet()) {
            TermCode termCode = TERM_CODES.get(key);
            Matcher matcher = termCode.pattern.matcher(formatted);
            if (matcher.find()) {
                if (("warn".equals(key) && !warnLevel.get()) || ("info".equals(key) && !infoLevel.get())
                        || ("dbug".equals(key) && !dbugLevel.get())) {
                    // this is a log statement for a disabled log-level, skip.
                    return null;
                }
                formatted = matcher.replaceAll(termCode.output);
            }
        }
        return formatted;
    }

    private Output() { }
}