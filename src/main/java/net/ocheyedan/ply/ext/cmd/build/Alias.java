package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import net.ocheyedan.ply.ext.props.Props;
import net.ocheyedan.ply.ext.props.Scope;

import java.util.*;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 12:41 PM
 */
final class Alias extends Script {

    /**
     * Thrown to indicate an alias has been defined to include itself.
     */
    @SuppressWarnings("serial")
    static class CircularReference extends RuntimeException {
        final String alias;
        CircularReference(String alias) {
            this.alias = alias;
        }
    }

    /**
     * @param scope from which to find alias {@code named}
     * @param named the name of the alias to find within {@code scope}
     * @return the {@link Alias} named {@code named} from {@code scope}
     */
    static Alias getAlias(Scope scope, String named) {
        Map<String, Alias> aliases = getAliases(scope);
        return aliases.get(named);
    }

    /**
     * @param scope from which to get aliases
     * @return all aliases defined within {@code scope}
     */
    static Map<String, Alias> getAliases(Scope scope) {
        Map<String, Prop> unparsedAliases = getUnparsedAliases(scope);
        Map<String, Alias> map = new HashMap<String, Alias>(unparsedAliases.size());
        for (Prop prop : unparsedAliases.values()) {
            try {
                parseAlias(prop.name, prop.value, unparsedAliases, map, new HashSet<String>());
            } catch (CircularReference cr) {
                Output.print("^error^ Alias (^b^%s^r^) contains a circular reference (run '^b^ply get %s from aliases^r^' to analyze).", cr.alias, cr.alias);
                System.exit(1);
            }
        }
        return map;
    }

    static Alias parseAlias(String name, String value, Map<String, Prop> unparsedAliases, Map<String, Alias> aliases, Set<String> encountered) {
        encountered.add(name);
        List<Script> scripts = parse(value, aliases, unparsedAliases, encountered);
        Alias alias = new Alias(name, scripts);
        aliases.put(name, alias);
        return alias;
    }

    private static List<Script> parse(String value, Map<String, Alias> aliases, Map<String, Prop> unparsedAliases,
                                      Set<String> encountered) {
        List<String> scripts = splitScript(value);
        List<Script> parsedScripts = new ArrayList<Script>(scripts.size());
        for (String script : scripts) {
            if (encountered.contains(script)) {
                throw new CircularReference(script);
            }
            if (aliases.containsKey(script)) {
                parsedScripts.add(aliases.get(script));
            } else if (unparsedAliases.containsKey(script)) {
                parsedScripts.add(parseAlias(script, unparsedAliases.get(script).value, unparsedAliases, aliases, encountered));
            } else {
                parsedScripts.add(new Script(script));
            }
        }
        return parsedScripts;
    }

    private static Map<String, Prop> getUnparsedAliases(Scope scope) {
        Collection<Prop> props = Props.get(new Context("aliases"), scope);
        Map<String, Prop> map = new HashMap<String, Prop>(props.size());
        for (Prop prop : props) {
            map.put(prop.name, prop);
        }
        return map;
    }

    private static List<String> splitScript(String script) {
        List<String> matchList = new ArrayList<String>();
        StringBuilder buffer = new StringBuilder();
        boolean withinQuotations = false;
        char[] characters = script.toCharArray();
        // split by spaces, ignoring spaces within quotation marks
        for (int i = 0; i < characters.length; i++) {
            char cur = characters[i];
            if ((' ' == cur) && !withinQuotations) {
                matchList.add(buffer.toString());
                buffer = new StringBuilder();
            } else if (('"' == cur)
                    && ((buffer.length() == 0) || ((i == (characters.length - 1)) || (' ' == characters[i + 1])))) {
                withinQuotations = (buffer.length() == 0);
            } else {
                buffer.append(cur);
            }
        }
        matchList.add(buffer.toString());
        return matchList;
    }

    final List<Script> scripts;

    Alias(String name, List<Script> scripts) {
        super(name);
        this.scripts = scripts;
    }


}
