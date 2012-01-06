package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.Iter;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.CommandLineParser;
import net.ocheyedan.ply.ext.props.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Vertex;

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
     * Cache of resolved aliases for a particular scope.
     */
    static final Map<Scope, Map<String, Alias>> cache = new HashMap<Scope, Map<String, Alias>>();
    static final Map<Scope, Map<String, Prop>> mappedPropCache = new HashMap<Scope, Map<String, Prop>>();

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
                Script parsed = Script.parse(prop.name, scope);
                parseAlias(scope, parsed, prop.value, unparsedAliases, new DirectedAcyclicGraph<String>(), map);
            } catch (CircularReference cr) {
                Output.print("^error^ Alias (^b^%s^r^) contains a circular reference (run '^b^ply get %s from aliases^r^' to analyze).", cr.alias, cr.alias);
                System.exit(1);
            }
        }
        cache.put(scope, map);
        return map;
    }

    static Alias parseAlias(Scope scope, Script parsed, String value, Map<String, Prop> unparsedAliases,
                            DirectedAcyclicGraph<String> cycleDetector, Map<String, Alias> parsedAliases) {
        cycleDetector.addVertex(parsed.name);
        List<Script> scripts = parse(scope, parsed.scope, parsed.name, value, unparsedAliases, cycleDetector, parsedAliases);
        Alias alias = new Alias(parsed.name, parsed.scope, scripts, parsed.arguments, parsed.unparsedName);
        parsedAliases.put(parsed.name, alias);
        return alias;
    }

    private static List<Script> parse(Scope originalScope, Scope scope, String name, String value,
                                      Map<String, Prop> unparsedAliases, DirectedAcyclicGraph<String> cycleDetector,
                                      Map<String, Alias> parsedAliases) {
        Args args = CommandLineParser.parseArgs(Iter.sized(splitScript(value)));
        AdHoc.add(args.adHocProps);
        AdHoc.merge();
        List<String> scripts = args.args;
        List<Script> parsedScripts = new ArrayList<Script>(scripts.size());
        Vertex<String> aliasVertex = cycleDetector.getVertex(name);
        for (String script : scripts) {
            Script parsed = Script.parse(script, scope);
            Vertex<String> scriptVertex = cycleDetector.addVertex(parsed.name);
            try {
                cycleDetector.addEdge(aliasVertex, scriptVertex);
            } catch (Graph.CycleException gce) {
                throw new CircularReference(parsed.name);
            }
            if (!parsed.scope.equals(originalScope)) {
                Map<String, Prop> scopedUnparsedAliases = getUnparsedAliases(parsed.scope);
                if (scopedUnparsedAliases.containsKey(parsed.name)) {
                    Alias alias = parseAlias(parsed.scope, parsed, scopedUnparsedAliases.get(parsed.name).value,
                               scopedUnparsedAliases, new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>());
                    parsedScripts.add(alias.augment(parsed.arguments, parsed.unparsedName));
                } else {
                    parsedScripts.add(parsed);
                }
            } else {
                if (parsedAliases.containsKey(parsed.name)) {
                    parsedScripts.add(parsedAliases.get(parsed.name).augment(parsed.arguments, parsed.unparsedName));
                } else if (unparsedAliases.containsKey(parsed.name)) {
                    Alias alias = parseAlias(parsed.scope, parsed, unparsedAliases.get(parsed.name).value,
                                             unparsedAliases, cycleDetector, parsedAliases);
                    parsedScripts.add(alias.augment(parsed.arguments, parsed.unparsedName));
                } else {
                    parsedScripts.add(parsed);
                }
            }
        }
        return parsedScripts;
    }

    private static Map<String, Prop> getUnparsedAliases(Scope scope) {
        if (mappedPropCache.containsKey(scope)) {
            return mappedPropCache.get(scope);
        }
        Collection<Prop> props = Props.get(new Context("aliases"), scope);
        Map<String, Prop> map = new HashMap<String, Prop>(props.size());
        for (Prop prop : props) {
            map.put(prop.name, prop);
        }
        mappedPropCache.put(scope, map);
        return map;
    }

    final List<Script> scripts;

    Alias(String name, Scope scope, List<Script> scripts, List<String> arguments, String unparsed) {
        super(name, scope, arguments, unparsed);
        this.scripts = scripts;
    }

    Alias with(List<Script> scripts) {
        return new Alias(this.name, this.scope, scripts, this.arguments, this.unparsedName);
    }

    Alias augment(List<String> arguments, String unparsedName) {
        List<String> copiedArguments = new ArrayList<String>(this.arguments); // add this.arguments first
        copiedArguments.addAll(arguments);
        return new Alias(this.name, this.scope, this.scripts, copiedArguments, unparsedName);
    }

}
