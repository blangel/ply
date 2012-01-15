package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Iter;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.CommandLineParser;
import net.ocheyedan.ply.ext.exec.Execution;
import net.ocheyedan.ply.ext.props.*;
import net.ocheyedan.ply.graph.DirectedAcyclicGraph;
import net.ocheyedan.ply.graph.Graph;
import net.ocheyedan.ply.graph.Vertex;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 1/2/12
 * Time: 12:41 PM
 */
public final class Alias extends Script {

    /**
     * Thrown to indicate an alias has been defined to include itself.
     */
    @SuppressWarnings("serial")
    static final class CircularReference extends RuntimeException {
        final String alias;
        CircularReference(String alias) {
            this.alias = alias;
        }
    }

    /**
     * Resolves aliases for a given {@code Scope} and config-directory.
     */
    static final class Resolver {

        /**
         * Cache of resolved aliases for a particular scope.
         */
        final Map<Scope, Map<String, Alias>> cache = new HashMap<Scope, Map<String, Alias>>();
        final Map<Scope, Map<String, Prop>> mappedPropCache = new HashMap<Scope, Map<String, Prop>>();

        final File configDirectory;

        /**
         * @param configDirectory the project configuration directory from which to get aliases
         */
        Resolver(File configDirectory) {
            this.configDirectory = configDirectory;
        }

        /**
         * @param scope from which to get aliases
         * @return all aliases defined within {@code scope}
         */
        Map<String, Alias> getAliases(Scope scope) {
            if (cache.containsKey(scope)) {
                return cache.get(scope);
            }
            Map<String, Prop> unparsedAliases = getUnparsedAliases(scope);
            Map<String, Alias> map = new HashMap<String, Alias>(unparsedAliases.size());
            for (Prop prop : unparsedAliases.values()) {
                try {
                    Script parsed = Script.parse(prop.name, scope);
                    parseAlias(scope, parsed, prop.value, unparsedAliases, new DirectedAcyclicGraph<String>(), map, Collections.<String>emptyList());
                } catch (CircularReference cr) {
                    Output.print("^error^ Alias (^b^%s^r^) contains a circular reference (run '^b^ply get %s from aliases^r^' to analyze).", cr.alias, cr.alias);
                    System.exit(1);
                }
            }
            cache.put(scope, map);
            return map;
        }

        Alias parseAlias(Scope scope, Script parsed, String value, Map<String, Prop> unparsedAliases,
                         DirectedAcyclicGraph<String> cycleDetector, Map<String, Alias> parsedAliases, List<String> adHocProps) {
            cycleDetector.addVertex(parsed.name);
            List<Script> scripts = parse(scope, parsed.scope, parsed.name, value, unparsedAliases, cycleDetector, parsedAliases);
            Alias alias = new Alias(parsed.name, parsed.scope, scripts, parsed.arguments, adHocProps, parsed.unparsedName);
            parsedAliases.put(parsed.name, alias);
            return alias;
        }

        private List<Script> parse(Scope originalScope, Scope scope, String name, String value,
                                   Map<String, Prop> unparsedAliases, DirectedAcyclicGraph<String> cycleDetector,
                                   Map<String, Alias> parsedAliases) {
            Args args = CommandLineParser.parseArgs(Iter.sized(splitScript(value)));
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
                                scopedUnparsedAliases, new DirectedAcyclicGraph<String>(), new HashMap<String, Alias>(),
                                args.adHocProps);
                        parsedScripts.add(alias.augment(parsed.arguments, parsed.unparsedName));
                    } else {
                        parsedScripts.add(parsed);
                    }
                } else {
                    if (parsedAliases.containsKey(parsed.name)) {
                        parsedScripts.add(parsedAliases.get(parsed.name).augment(parsed.arguments, parsed.unparsedName));
                    } else if (unparsedAliases.containsKey(parsed.name)) {
                        Alias alias = parseAlias(parsed.scope, parsed, unparsedAliases.get(parsed.name).value,
                                unparsedAliases, cycleDetector, parsedAliases, args.adHocProps);
                        parsedScripts.add(alias.augment(parsed.arguments, parsed.unparsedName));
                    } else {
                        parsedScripts.add(parsed);
                    }
                }
            }
            return parsedScripts;
        }

        private Map<String, Prop> getUnparsedAliases(Scope scope) {
            if (mappedPropCache.containsKey(scope)) {
                return mappedPropCache.get(scope);
            }
            Collection<Prop> props = Props.get(new Context("aliases"), configDirectory, scope);
            Map<String, Prop> map = new HashMap<String, Prop>(props.size());
            for (Prop prop : props) {
                map.put(prop.name, prop);
            }
            mappedPropCache.put(scope, map);
            return map;
        }

    }

    /**
     * Map from canonical-path of the config-directory to the {@link Resolver}.
     */
    static final Map<String, Resolver> cache = new HashMap<String, Resolver>();

    /**
     * @param configDirectory the project configuration directory from which to get an alias named {@code named}
     * @param scope from which to find alias {@code named}
     * @param named the name of the alias to find within {@code scope}
     * @return the {@link Alias} named {@code named} defined from {@code scope}
     */
    static Alias getAlias(File configDirectory, Scope scope, String named) {
        Map<String, Alias> aliases = getAliases(configDirectory, scope);
        return aliases.get(named);
    }

    /**
     * @param configDirectory the project configuration directory from which to get aliases
     * @param scope from which to find aliases
     * @return the aliases within {@code configDirectory} for the provided {@code scope}
     */
    static Map<String, Alias> getAliases(File configDirectory, Scope scope) {
        String cacheKey = FileUtil.getCanonicalPath(configDirectory);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey).getAliases(scope);
        }
        Resolver resolver = new Resolver(configDirectory);
        cache.put(cacheKey, resolver);
        return resolver.getAliases(scope);
    }

    final List<Script> scripts;

    /**
     * Alias can be defined with ad-hoc properties.
     */
    final List<String> adHocProps;

    Alias(String name, Scope scope, List<Script> scripts, List<String> arguments, List<String> adHocProps, String unparsed) {
        super(name, scope, arguments, unparsed);
        this.scripts = scripts;
        this.adHocProps = new ArrayList<String>(adHocProps);
    }

    @Override Script with(File location) {
        throw new UnsupportedOperationException("An Alias cannot have a location!");
    }

    /**
     * Converts each {@link #scripts} into {@link Execution} objects with this object as the {@link Execution#script}
     * value.  Note, if an entry within {@link #scripts} is itself an {@link Alias} object the converted
     * {@link Execution} objects' {@link Execution#script} will be that {@link Alias} and not this object.
     * @return the converted execution objects.
     */
    @Override List<Execution> convert() {
        List<Execution> executions = new ArrayList<Execution>(scripts.size()); // size is approx. as scripts may contain aliases
        for (Script script : scripts) {
            executions.addAll(script.convert(name));
        }
        // TODO - set alias's arguments via policy. currently only the last script gets the alias's arguments, policy
        // TODO - could dictate all scripts get alias's arguments
        if (!this.arguments.isEmpty()) {
            Execution last = executions.get(executions.size() - 1);
            last = last.augment(this.arguments.toArray(new String[this.arguments.size()]));
            executions.set(executions.size() - 1, last);
        }
        return executions;
    }

    /**
     * Converts each {@link #scripts} into {@link Execution} objects with this object as the {@link Execution#script}
     * value.  Note, if an entry within {@link #scripts} is itself an {@link Alias} object the converted
     * {@link Execution} objects' {@link Execution#script} will be that {@link Alias} and not this object.
     * @param overriddenExecutionName is ignored as {@link Alias} objects cannot have their generated {@link Execution#name}
     *        values overridden, that value will always be the {@link Alias#name} itself.
     * @return the converted execution objects.
     */
    @Override protected List<Execution> convert(String overriddenExecutionName) {
        return convert();
    }

    Alias with(List<Script> scripts) {
        return new Alias(this.name, this.scope, scripts, this.arguments, this.adHocProps, this.unparsedName);
    }

    Alias augment(List<String> arguments, String unparsedName) {
        List<String> copiedArguments = new ArrayList<String>(this.arguments); // add this.arguments first
        copiedArguments.addAll(arguments);
        return new Alias(this.name, this.scope, this.scripts, copiedArguments, this.adHocProps, unparsedName);
    }

}
