package net.ocheyedan.ply.script;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 9/22/11
 * Time: 6:32 PM
 */
public class FormattedDiagnosticListener implements DiagnosticListener<JavaFileObject> {

    public static enum Type {
        Error, Warning, Note
    }

    private final String srcPath;

    private final Map<Type, Set<String>> statements;

    public FormattedDiagnosticListener(String srcPath) {
        this.srcPath = srcPath;
        this.statements = new HashMap<Type, Set<String>>();
    }

    @Override public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
        // odd - mac-osx's compiler appears to report a Diagnostic on ignored warnings (ignored via compiler options)
        // but the source is null, skip these.
        if ((diagnostic == null) || (diagnostic.getSource() == null)) {
            return;
        }

        String kind, pad = " ", color = "blue";
        Type type = Type.Note;
        switch (diagnostic.getKind()) {
            case ERROR:
                kind = "error";
                pad = "  ";
                color = "red";
                type = Type.Error;
                break;
            case MANDATORY_WARNING:
            case WARNING:
                kind = "warning";
                color = "yellow";
                type = Type.Warning;
                break;
            default:
                kind = "message";
        }
        String className = diagnostic.getSource().toUri().toString();
        className = className.replace(srcPath, "").replace(".java", "").replaceAll(File.separator, ".");
        String classShortName = (className.lastIndexOf(".") != -1) ? className.substring(className.lastIndexOf(".") + 1) : className;
        classShortName = classShortName.replace("$", ".");
        className = className.replace("$", ".");

        String lineNumber = String.valueOf(diagnostic.getLineNumber());

        String message = diagnostic.getMessage(null);
        int index = message.lastIndexOf(kind + ": ");
        if (index != -1) {
            message = message.substring(index + kind.length() + 2);
        } else {
            index = message.lastIndexOf(lineNumber + ": ");
            if (index != -1) {
                message = message.substring(index + lineNumber.length() + 2);
            }
        }
        message = message.replaceAll("\\n", " ");
        message = message.replaceAll(" found   :", "; found^b^");
        message = message.replaceAll("required:", "^r^required^b^");
        message = message.replaceAll("\\[unchecked\\] ", "");
        message = message.replaceAll("\\[serial\\] ", "");
        message = message.replaceAll(" symbol  :", ";^b^");
        message = message.replaceAll("location:", "^r^in^b^");
        // since we're printing the className at the end of every message, if the className is within the message
        // replace it with the shortClassName (i.e., className without package) for readability.
        message = message.replaceAll(Pattern.quote(className), Matcher.quoteReplacement(classShortName));

        Set<String> messages = statements.get(type);
        if (messages == null) {
            messages = new HashSet<String>();
            statements.put(type, messages);
        }
        messages.add(String.format("^%s^^i^%s%s%s^r^ %s^r^ @ line ^b^%s^r^ in ^b^%s^r^", color, pad, kind, pad,
                message, lineNumber, className));
    }

    public Set<String> getErrors() {
        return getType(Type.Error);
    }

    public Set<String> getWarnings() {
        return getType(Type.Warning);
    }

    public Set<String> getNotes() {
        return getType(Type.Note);
    }

    private Set<String> getType(Type type) {
        Set<String> messages = statements.get(type);
        if (messages == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(messages);
        }
    }

}
