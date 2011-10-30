package net.ocheyedan.ply.script;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import java.io.File;
import java.util.*;

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
        String kind = "", pad = " ", color = "blue";
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
        className = className.replace(srcPath, "").replace(".java", "").replaceAll(File.separator, ".").replace("$", ".");

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
