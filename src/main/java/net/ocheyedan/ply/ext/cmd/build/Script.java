package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.props.Scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 12:41 PM
 */
class Script {

    /**
     * @see #splitScript(String)
     */
    static final Pattern SPLIT_REG_EX = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    /**
     * Splits {@code script} by ' ', ignoring space characters within quotation marks.
     * @param script to split
     * @return the split list of {@code script}
     */
    static List<String> splitScript(String script) {
        if (script == null) {
            return Collections.emptyList();
        }
        List<String> matchList = new ArrayList<String>();
        Matcher regexMatcher = SPLIT_REG_EX.matcher(script);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                // Add double-quoted string without the quotes
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                // Add single-quoted string without the quotes
                matchList.add(regexMatcher.group(2));
            } else {
                // Add unquoted word
                matchList.add(regexMatcher.group());
            }
        }
        return matchList;
    }

    /**
     * @param script to parse which is in the form: [scope:]scriptName [arg0...argn]
     * @param defaultScope to be used if {@code script} does not contain scope information
     * @return the parsed {@code script} which may have been prefixed with a scope (in the form 'scope:script').
     */
    static Script parse(String script, Scope defaultScope) {
        if ((script == null) || !script.contains(":")) {
            return ((script == null) || script.isEmpty() ? null : parseArgs(script, defaultScope, script));
        }
        String unparsedName = script;
        // script contains ':' only use if it occurs before a break-char (' ', '\'', '"')
        int scopeIndex = -1;
        loop:for (char character : script.toCharArray()) {
            scopeIndex++;
            switch (character) {
                case ':':
                    break loop;
                case ' ':
                case '\'':
                case '"':
                    scopeIndex = -1;
                    break loop;
            }
        }
        Scope scope;
        if (scopeIndex == -1) {
            scope = defaultScope;
        } else if (scopeIndex == 0) {
            scope = Scope.Default; // user explicitly asked for default; ':scriptName'
        } else {
            scope = new Scope(script.substring(0, scopeIndex));
            script = script.substring(scopeIndex + 1);
        }
        return parseArgs(script, scope, unparsedName);
    }

    static Script parseArgs(String script, Scope scope, String unparsedName) {
        // if there are spaces within the script then everything after the first result is considered to be
        // explicit arguments passed to the script/alias; i.e., script=compile arg1 arg2 means the user
        // typed "compile arg1 arg2" on the command line
        List<String> scripts = splitScript(script);
        if ((scripts == null) || scripts.isEmpty()) {
            throw new AssertionError(String.format("Parsing %s should have created at least one script.", script));
        } else if (scripts.size() == 1) {
            return new Script(scripts.get(0), scope, unparsedName);
        } else {
            script = scripts.remove(0);
            return new Script(script, scope, scripts, unparsedName);
        }
    }

    final String name;

    final String unparsedName;

    final Scope scope;

    final List<String> arguments;

    Script(String name, Scope scope, String unparsedName) {
        this(name, scope, Collections.<String>emptyList(), unparsedName);
    }

    Script(String name, Scope scope, List<String> arguments, String unparsedName) {
        this.name = name;
        this.scope = scope;
        this.arguments = new ArrayList<String>(arguments); // copy, so as to allow append
        this.unparsedName = unparsedName;
    }

    @Override public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Script script = (Script) o;

        if (name != null ? !name.equals(script.name) : script.name != null) {
            return false;
        }
        return (scope == null ? script.scope == null : scope.equals(script.scope));
    }

    @Override public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (scope != null ? scope.hashCode() : 0);
        return result;
    }
}
