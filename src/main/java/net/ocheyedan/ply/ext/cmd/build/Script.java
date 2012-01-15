package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.exec.Execution;
import net.ocheyedan.ply.ext.props.Scope;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 12:41 PM
 * 
 * Note, any script or alias can be prefixed with a scope via 'scope_name:'.  This scope will determine the
 * set of resolved properties to pass to the execution.  If nothing is specified, it is the default scope and
 * so the default scoped properties are used (i.e., for context 'compiler' with default scope the properties are
 * resolved from the config dir from file 'compiler.properties').  If a scope is specified then that scope's properties
 * (inheriting from default if not overridden explicitly by the scope) are used (i.e., for context 'compiler' with
 * scope 'test' then the properties are resolved from the config dir from file 'compiler.test.properties' and if there
 * are any properties within the default, 'compiler.properties' which are not present in the scoped
 * 'compiler.test.properties' then they are also included).
 * For an alias with a scope then every script defined by the alias also has the scope.  For instance, say there exists
 * an alias 'install' which resolves to scripts 'file-changed compile package'.  If 'test:install' is invoked, in other
 * words the install alias is invoked with 'test' scope, then the resolved scripts to be invoked would be
 * 'test:file-changed test:compile test:package'.
 */
public class Script {

    /**
     * @see #splitScript(String)
     */
    static final Pattern SPLIT_REG_EX = Pattern.compile("[^\\s\"'`]+|(`[^`]*`)|\"([^\"]*)\"|'([^']*)'");

    /**
     * Splits {@code script} by ' ', ignoring space characters within quotation or tick marks.
     * @param script to split
     * @return the split list of {@code script}
     */
    public static List<String> splitScript(String script) {
        if (script == null) {
            return Collections.emptyList();
        }
        List<String> matchList = new ArrayList<String>();
        Matcher regexMatcher = SPLIT_REG_EX.matcher(script);
        while (regexMatcher.find()) {
            String match = regexMatcher.group(1);
            if (match != null) {
                // Add tick string with the ticks
                matchList.add(match);
            } else if ((match = regexMatcher.group(2)) != null) {
                // Add double-quoted string without the quotes
                matchList.add(match);
            } else if ((match = regexMatcher.group(3)) != null) {
                // Add single-quoted string without the quotes
                matchList.add(match);
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
        // script contains ':' only use if it occurs before a break-char (' ', '\'', '"', '`')
        int scopeIndex = -1;
        loop:for (char character : script.toCharArray()) {
            scopeIndex++;
            switch (character) {
                case ':':
                    break loop;
                case ' ':
                case '\'':
                case '"':
                case '`':
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

    public final String name;

    public final String unparsedName;

    public final Scope scope;

    final List<String> arguments;

    /**
     * The location of the actual script (from the local project or the system defaults)
     */
    final File location;

    Script(String name, Scope scope, String unparsedName) {
        this(name, scope, Collections.<String>emptyList(), unparsedName, null);
    }

    Script(String name, Scope scope, List<String> arguments, String unparsedName) {
        this(name, scope, arguments, unparsedName, null);
    }

    Script(String name, Scope scope, List<String> arguments, String unparsedName, File location) {
        this.name = name;
        this.scope = scope;
        this.arguments = new ArrayList<String>(arguments); // copy, so as to allow append
        this.unparsedName = unparsedName;
        this.location = location;
    }

    Script with(File location) {
        return new Script(this.name, this.scope, this.arguments, this.unparsedName, location);
    }

    /**
     * @return this script converted into an {@link Execution} (the list is one-sized).
     */
    List<Execution> convert() {
        return convert(name);
    }

    /**
     * Allows subclasses to specify a different execution name for the converted {@link Execution}.  This is useful
     * for {@link Alias} objects as the {@link Execution} will use the {@link Execution#name} property when printing what
     * is being executed and for an {@link Alias} it should be it and not the resolved scripts (i.e., print out
     * that 'clean' is being run even though 'rm -rf ${target}' is being run).
     * @param overriddenExecutionName to use in the converted {@link Execution} objects' {@link Execution#name} values.
     * @return the script converted into an {@link Execution}.
     */
    protected List<Execution> convert(String overriddenExecutionName) {
        // ensure a location has been specified and that it is executable
        if ((location == null) || !location.canExecute()) {
            Output.print("^error^ Found script ^b^%s^r^%s but it is not executable.", name,
                    Scope.Default.equals(scope) ? "" : String.format(" (in scope ^b^%s^r^)", scope));
            System.exit(1);
        }
        String[] executableArgs = new String[arguments.size() + 1];
        executableArgs[0] = FileUtil.getCanonicalPath(location);
        for (int i = 1; i < executableArgs.length; i++) {
            executableArgs[i] = arguments.get(i - 1);
        }
        List<Execution> executions = new ArrayList<Execution>(1);
        executions.add(new Execution(overriddenExecutionName, this, executableArgs));
        return executions;
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
