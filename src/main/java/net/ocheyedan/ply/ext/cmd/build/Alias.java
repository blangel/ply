package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.Iter;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.CommandLineParser;
import net.ocheyedan.ply.ext.props.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Vertex;
import net.ocheyedan.ply.props.Loader;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Cache of resolved aliases for a particular scope.
     */
    static final Map<Scope, Map<String, Alias>> cache = new HashMap<Scope, Map<String, Alias>>();

    /**
     * @param scope from which to find alias {@code named}
     * @param named the name of the alias to find within {@code scope}
     * @return the {@link Alias} named {@code named} defined from {@code scope}
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
        if (cache.containsKey(scope)) {
            return cache.get(scope);
        }
        Map<String, Prop> unparsedAliases = getUnparsedAliases(scope);
        Map<String, Alias> map = new HashMap<String, Alias>(unparsedAliases.size());
        for (Prop prop : unparsedAliases.values()) {
            try {
                Alias alias = parseAlias(scope, prop.name, prop.value, unparsedAliases, new DirectedAcyclicGraph<String>());
                map.put(alias.name, alias);
            } catch (CircularReference cr) {
                Output.print("^error^ Alias (^b^%s^r^) contains a circular reference (run '^b^ply get %s from aliases^r^' to analyze).", cr.alias, cr.alias);
                System.exit(1);
            }
        }
        cache.put(scope, map);
        return map;
    }

    static Alias parseAlias(Scope scope, String name, String value, Map<String, Prop> unparsedAliases,
                            DirectedAcyclicGraph<String> cycleDetector) {
        Script parsed = Script.parse(name, scope);
        name = parsed.name;
        Scope aliasScope = parsed.scope;
        cycleDetector.addVertex(name);
        List<Script> scripts = parse(scope, aliasScope, name, value, unparsedAliases, cycleDetector);
        return new Alias(name, aliasScope, scripts);
    }

    private static List<Script> parse(Scope originalScope, Scope scope, String name, String value,
                                      Map<String, Prop> unparsedAliases, DirectedAcyclicGraph<String> cycleDetector) {
        Args args = CommandLineParser.parseArgs(Iter.sized(splitScript(value)));
        AdHoc.add(args.adHocProps);
        AdHoc.merge();
        List<String> scripts = args.args;
        List<Script> parsedScripts = new ArrayList<Script>(scripts.size());
        Vertex<String> aliasVertex = cycleDetector.getVertex(name);
        for (String script : scripts) {
            Script parsed = Script.parse(script, scope);
            Scope scriptScope = parsed.scope;
            script = parsed.name;
            Vertex<String> scriptVertex = cycleDetector.addVertex(script);
            try {
                cycleDetector.addEdge(aliasVertex, scriptVertex);
            } catch (Graph.CycleException gce) {
                throw new CircularReference(script);
            }
            if (!scriptScope.equals(originalScope)) {
                Map<String, Alias> scopedAliases = getAliases(scriptScope);
                if (scopedAliases.containsKey(script)) {
                    parsedScripts.add(scopedAliases.get(script));
                } else {
                    parsedScripts.add(new Script(script, scriptScope));
                }
            } else {
                if (unparsedAliases.containsKey(script)) {
                    Alias alias = parseAlias(scriptScope, script, unparsedAliases.get(script).value, unparsedAliases,
                                             cycleDetector);
                    parsedScripts.add(alias);
                } else {
                    parsedScripts.add(new Script(script, scriptScope));
                }
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

    final List<Script> scripts;

    Alias(String name, Scope scope, List<Script> scripts) {
        super(name, scope);
        this.scripts = scripts;
    }


}
