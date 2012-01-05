package net.ocheyedan.ply.ext.cmd.build;

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
     * @param script to parse which is in the form: [scope:]scriptName
     * @param defaultScope to be used if {@code script} does not contain scope information
     * @return the parsed {@code script} which may have been prefixed with a scope (in the form 'scope:script').
     */
    static Script parse(String script, Scope defaultScope) {
        if ((script == null) || !script.contains(":")) {
            return new Script(script, defaultScope);
        }
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
        if (scopeIndex == -1) {
            return new Script(script, defaultScope);
        } else if (scopeIndex == 0) {
            return new Script(script, Scope.Default);
        } else {
            return new Script(script.substring(scopeIndex + 1), new Scope(script.substring(0, scopeIndex)));
        }
    }

    final String name;

    final Scope scope;

    Script(String name, Scope scope) {
        this.name = name;
        this.scope = scope;
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
