package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 12:50 PM
 *
 * Filters {@link Prop} objects in-place with properties from themselves as well as from environment variables.
 */
public final class Filter {

    private static final Map<String, String> _cache = new ConcurrentHashMap<String, String>();

    /**
     * Filters {@code toFilter} against the properties within {@code props} and any environment variables.
     * @param context the context for which to first filter without prefixing filter values with a context
     * @param toFilter to filter
     * @param props from which to filter (which themselves should already be filtered)
     * @return the filtered value
     */
    public static String filter(Context context, String toFilter, Map<Context, Collection<Prop>> props) {
        if ((toFilter == null) || (props == null || !toFilter.contains("${"))) {
            return toFilter;
        }
        // first filter by passed-in context
        if ((context != null) && props.containsKey(context)) {
            for (Prop prop : props.get(context)) {
                toFilter = filterValue(toFilter, prop, "");
                if (!toFilter.contains("${")) { // short-circuit if nothing left to filter
                    return toFilter;
                }
            }
        }
        // next filter through each context, with context-prefix values
        for (Context curContext : props.keySet()) {
            for (Prop prop : props.get(curContext)) {
                toFilter = filterValue(toFilter, prop, curContext + ".");
                if (!toFilter.contains("${")) { // short-circuit if nothing left to filter
                    return toFilter;
                }
            }
        }
        // finally, check the environment variables
        for (String envPropKey : System.getenv().keySet()) {
            if (toFilter.contains("${" + envPropKey + "}")) {
                try {
                    toFilter = toFilter.replaceAll(Pattern.quote("${" + envPropKey + "}"), System.getenv(envPropKey));
                } catch (IllegalArgumentException iae) {
                    Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", envPropKey, System.getenv(envPropKey));
                    Output.print(iae);
                }
                if (!toFilter.contains("${")) { // short-circuit if nothing left to filter
                    return toFilter;
                }
            }
        }

        return toFilter;
    }

    private static String filterValue(String toFilter, Prop by, String prefix) {
        try {
            return toFilter.replaceAll(Pattern.quote("${" + prefix + by.name + "}"), by.value);
        } catch (IllegalArgumentException iae) {
            Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", prefix + by.name, by.value);
            Output.print(iae);
            return toFilter;
        }
    }

    /**
     * Filters {@code props} in place.
     * @param configDirectory from which to extract local project properties
     * @param props to filter with values from itself as well as all available environment variables.
     */
    static void filter(File configDirectory, Collection<Prop.All> props) {
        if ((configDirectory == null) || (props == null)) {
            return;
        }
        Set<Scope> scopes = collectScopes(props);
        Set<Context> contexts = collectContexts(props);
        for (Scope scope : scopes) {
            for (Prop.All prop : props) {
                Prop.Val propVal = prop.get(scope);
                String value = (propVal == null ? null : propVal.unfiltered);
                if ((value == null) || !value.contains("${")) {
                    continue;
                }
                String cacheKey = getKey(configDirectory, scope, prop.context, value);
                String filteredValue;
                if (_cache.containsKey(cacheKey)) {
                    filteredValue = _cache.get(cacheKey);
                } else {
                    filteredValue = filter(prop.context, scope, value, props, contexts);
                    if (Output.isDebug()) {
                        // don't output large values in their entirety
                        String outputFiltered = filteredValue, outputValue = value;
                        if (filteredValue.length() > 80) {
                            outputFiltered = filteredValue.substring(0, 80) + " [truncated]";
                        }
                        if (value.length() > 80) {
                            outputValue = value.substring(0, 80) + " [truncated]";
                        }
                        Output.print("^dbug^ filtered ^b^%s^r^ to ^b^%s^r^ [ in %s ].", outputValue, outputFiltered, prop.context.name);
                    }
                    _cache.put(cacheKey, filteredValue);
                }
                prop.set(propVal.from, filteredValue, value);
            }
        }
    }

    private static Set<Scope> collectScopes(Collection<Prop.All> props) {
        Set<Scope> scopes = new HashSet<Scope>();
        for (Prop.All prop : props) {
            scopes.addAll(prop.getScopes());
        }
        return scopes;
    }

    private static Set<Context> collectContexts(Collection<Prop.All> props) {
        Set<Context> contexts = new HashSet<Context>();
        for (Prop.All prop : props) {
            contexts.add(prop.context);
        }
        return contexts;
    }

    /**
     * Filters {@code value} by values within {@code props}.  The owning context is {@code context}.
     * @param context which owns {@code value}
     * @param scope for which to filter
     * @param value to filter
     * @param props from which to filter
     * @param contexts the unique set of contexts from {@code props}.
     * @return the filtered value
     */
    static String filter(Context context, Scope scope, String value, Collection<Prop.All> props, Set<Context> contexts) {
        if ((context == null) || (scope == null) || (value == null) || (props == null) || (contexts == null)
                || !value.contains("${")) {
            return value;
        }
        String filtered = value;
        // first, filter by the same context
        filtered = filterValue(context, scope, "", filtered, props, contexts);
        if (!filtered.contains("${")) { // short-circuit if nothing left to filter
            return filtered;
        }
        // next, filter through all other contexts (which must have context-prefixed values)
        for (Context otherContext : contexts) {
            if (context.equals(otherContext)) {
                continue;
            }
            filtered = filterValue(otherContext, scope, otherContext.name + ".", filtered, props, contexts);
            if (!filtered.contains("${")) { // short-circuit if nothing left to filter
                return filtered;
            }
        }
        // finally, filter by the environment variables
        for (String envPropKey : System.getenv().keySet()) {
            if (filtered.contains("${" + envPropKey + "}")) {
                try {
                    filtered = filtered.replaceAll(Pattern.quote("${" + envPropKey + "}"), System.getenv(envPropKey));
                } catch (IllegalArgumentException iae) {
                    Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", envPropKey, System.getenv(envPropKey));
                    Output.print(iae);
                }
                if (!filtered.contains("${")) { // short-circuit if nothing left to filter
                    return filtered;
                }
            }
        }

        return filtered;
    }

    static String filterValue(Context context, Scope scope, String prefix, String value, Collection<Prop.All> props,
                              Set<Context> contexts) {
        for (Prop.All prop : props) {
            if (!context.equals(prop.context)) {
                continue;
            }
            String toFind = prefix + prop.name;
            if (value.contains("${" + toFind + "}")) {
                Prop.Val propVal = prop.get(scope);
                String replacement = filter(context, scope, (propVal == null ? null : propVal.unfiltered), props, contexts); // TODO - circular prop defs
                try {
                    value = value.replaceAll(Pattern.quote("${" + toFind + "}"), replacement);
                } catch (IllegalArgumentException iae) {
                    Output.print("^error^ Error filtering '^b^%s^r^' with '^b^%s^r^'.", toFind, replacement);
                    Output.print(iae);
                }
            }
        }
        return value;
    }

    private static String getKey(File configDirectory, Scope scope, Context context, String unfilteredValue) {
        return Cache.getKey(configDirectory) + "#" + scope.name + "#" + context.name + "." + unfilteredValue;
    }

}
