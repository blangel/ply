package net.ocheyedan.ply;

import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
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
        private final String nonColoredOutput;
        private TermCode(Pattern pattern, String output, String nonColoredOutput) {
            this.pattern = pattern;
            this.output = output;
            this.nonColoredOutput = nonColoredOutput;
        }
    }

    /**
     * Used to queue messages before {@link Output#init()} has been called.
     */
    private static final class Message {
        private static enum Type { Line, NoLine, Exec }
        private final String message;
        private final Type type;
        private final Object[] args;
        private Message(String message, Type type, Object[] args) {
            this.message = message;
            this.type = type;
            this.args = args;
        }
    }

    /**
     * The queue of {@link Message} objects which have been accumulated before the {@link Output#init()} has been called.
     */
    private static final List<Message> queue = new ArrayList<Message>();

    /**
     * Set to true when {@link #init()} has been called.
     */
    private static volatile boolean inited = false;

    /**
     * Configurable log level variables.
     */
    private static final AtomicBoolean warnLevel = new AtomicBoolean(true);
    private static final AtomicBoolean infoLevel = new AtomicBoolean(true);
    private static final AtomicBoolean dbugLevel = new AtomicBoolean(true);
    /**
     * If true, color will be allowed within output.
     */
    private static final AtomicBoolean coloredOutput = new AtomicBoolean(true);
    /**
     * If false, no ply output will be applied and scripts' output will be printed as-is without any interpretation.
     */
    private static final AtomicBoolean decorated = new AtomicBoolean(true);
    /**
     * True if
     */
    private static final AtomicBoolean withinTerminal = new AtomicBoolean(true);

    /**
     * A mapping of easily identifiable words to a {@link TermCode} object for colored output.
     */
    private static final Map<String, TermCode> TERM_CODES = new HashMap<String, TermCode>();

    static void init() {
        Context plyContext = Context.named("ply");
        init(Props.getValue(plyContext, "color"), Props.getValue(plyContext, "decorated"),
             Props.getValue(plyContext, "log.levels"));
    }

    static void init(String coloredOutput, String decorated, String logLevels) {
        if (inited) {
            return;
        }
        try {
            String terminal = System.getenv("TERM");
            if (!logLevels.contains("warn")) {
                warnLevel.set(false);
            }
            if (!logLevels.contains("info")) {
                infoLevel.set(false);
            }
            if (!logLevels.contains("debug") && !logLevels.contains("dbug")) {
                dbugLevel.set(false);
            }
            if ("false".equalsIgnoreCase(decorated)) {
                Output.decorated.set(false);
            }
            withinTerminal.set(terminal != null);
            boolean useColor = withinTerminal.get() && !"false".equalsIgnoreCase(coloredOutput);
            Output.coloredOutput.set(useColor);
            // TODO - what are the range of terminal values and what looks best for each?
            String terminalBold = ("xterm".equals(terminal) ? "1" : "0");
            TERM_CODES.put("ply", new TermCode(Pattern.compile("\\^ply\\^"), "[\u001b[0;33mply\u001b[0m]", "[ply]"));
            TERM_CODES.put("error", new TermCode(Pattern.compile("\\^error\\^"), "[\u001b[1;31merr!\u001b[0m]", "[err!]"));
            TERM_CODES.put("warn", new TermCode(Pattern.compile("\\^warn\\^"), "[\u001b[1;33mwarn\u001b[0m]", "[warn]"));
            TERM_CODES.put("info", new TermCode(Pattern.compile("\\^info\\^"), "[\u001b[1;34minfo\u001b[0m]", "[info]"));
            TERM_CODES.put("dbug", new TermCode(Pattern.compile("\\^dbug\\^"), "[\u001b[1;30mdbug\u001b[0m]", "[dbug]"));
            TERM_CODES.put("reset", new TermCode(Pattern.compile("\\^r\\^"), "\u001b[0m", ""));
            TERM_CODES.put("bold", new TermCode(Pattern.compile("\\^b\\^"), "\u001b[1m", ""));
            TERM_CODES.put("normal", new TermCode(Pattern.compile("\\^n\\^"), "\u001b[2m", ""));
            TERM_CODES.put("inverse", new TermCode(Pattern.compile("\\^i\\^"), "\u001b[7m", ""));
            TERM_CODES.put("black", new TermCode(Pattern.compile("\\^black\\^"), "\u001b[" + terminalBold + ";30m", ""));
            TERM_CODES.put("red", new TermCode(Pattern.compile("\\^red\\^"),  "\u001b[" + terminalBold + ";31m", ""));
            TERM_CODES.put("green", new TermCode(Pattern.compile("\\^green\\^"), "\u001b[" + terminalBold + ";32m", ""));
            TERM_CODES.put("yellow", new TermCode(Pattern.compile("\\^yellow\\^"), "\u001b[" + terminalBold + ";33m", ""));
            TERM_CODES.put("blue", new TermCode(Pattern.compile("\\^blue\\^"), "\u001b[" + terminalBold + ";34m", ""));
            TERM_CODES.put("magenta", new TermCode(Pattern.compile("\\^magenta\\^"), "\u001b[" + terminalBold + ";35m", ""));
            TERM_CODES.put("cyan", new TermCode(Pattern.compile("\\^cyan\\^"), "\u001b[" + terminalBold + ";36m", ""));
            TERM_CODES.put("white", new TermCode(Pattern.compile("\\^white\\^"), "\u001b[" + terminalBold + ";37m", ""));
            drainQueue();
        } finally {
            inited = true;
        }
    }

    /**
     * Takes all the messages from {@link #queue} and calls the appropriate print method based on its {@link Message.Type}
     */
    private static void drainQueue() {
        if (inited) {
            return;
        }
        inited = true; // so that the re-call of the print methods actually prints
        for (Message message : queue) {
            switch (message.type) {
                case Line:
                    print(message.message, message.args); break;
                case NoLine:
                    printNoLine(message.message, message.args); break;
                case Exec:
                    printFromExec(message.message, message.args); break;
                default:
                    throw new AssertionError(String.format("Unknown Message.Type %s", message.type));
            }
        }
    }

    public static void print(String message, Object ... args) {
        if (!inited) {
            queue.add(new Message(message, Message.Type.Line, args));
            return;
        }
        String formatted = resolve(message, args);
        if ((formatted == null) || (!decorated.get() && isPrintFromPly())) {
            return;
        }
        System.out.println(formatted);
    }

    public static void printNoLine(String message, Object ... args) {
        if (!inited) {
            queue.add(new Message(message, Message.Type.NoLine, args));
            return;
        }
        String formatted = resolve(message, args);
        if ((formatted == null) || (!decorated.get() && isPrintFromPly())) {
            return;
        }
        System.out.print(formatted);
    }
    
    private static boolean isPrintFromPly() {
        // skip ply/ply-util print statements
        StackTraceElement[] stackTrace = new RuntimeException().getStackTrace(); // TODO - better (generic) way?
        if (stackTrace.length > 2) {
            String className = stackTrace[2].getClassName();
            if (className.startsWith("net.ocheyedan.ply") && !className.startsWith("net.ocheyedan.ply.script")) {
                return true;
            }
        }
        return false;
    }

    static void printFromExec(String message, Object ... args) {
        if (!inited) {
            queue.add(new Message(message, Message.Type.Exec, args));
            return;
        }
        String scriptArg = (String) args[1];
        if (!decorated.get()) {
            System.out.println(scriptArg);
            return;
        }
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

    static String resolve(String message, Object[] args) {
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
                if (decorated.get()) {
                    String output = isColoredOutput() ? termCode.output : termCode.nonColoredOutput;
                    formatted = matcher.replaceAll(output);
                }
            }
        }
        return formatted;
    }

    /**
     * @return true if warn level logging is enabled
     */
    public static boolean isWarn() {
        return warnLevel.get();
    }

    /**
     * @return true if info level logging is enabled
     */
    public static boolean isInfo() {
        return infoLevel.get();
    }

    /**
     * @return true if debug/dbug level logging is enabled
     */
    public static boolean isDebug() {
        return dbugLevel.get();
    }

    /**
     * @return true if the client can support colored output.
     */
    public static boolean isColoredOutput() {
        return coloredOutput.get();
    }

    /**
     * @return true if ply can print statements and scripts' output should be decorated (i.e., prefixed with script name, etc).
     */
    public static boolean isDecorated() {
        return decorated.get();
    }

    /**
     * Enable warn level logging.
     */
    public static void enableWarn() {
        warnLevel.set(true);
    }

    /**
     * Enables info level logging.
     */
    public static void enableInfo() {
        infoLevel.set(true);
    }

    /**
     * Enables debug/dbug level logging.
     */
    public static void enableDebug() {
        dbugLevel.set(true);
    }

    private Output() { }
}