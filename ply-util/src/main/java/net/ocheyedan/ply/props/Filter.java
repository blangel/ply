package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 12:50 PM
 *
 * Filters property values based on other properties and the system environment variables.
 */
public final class Filter {

    /**
     * Indicates a property value contains a circular reference.
     */
    @SuppressWarnings("serial")
    public static final class Circular extends RuntimeException { }

    private static final class FilterResult {
        private final String filteredResult;
        private final boolean filtered;
        private FilterResult(String filteredResult, boolean filtered) {
            this.filteredResult = filteredResult;
            this.filtered = filtered;
        }
    }

    private static final Map<String, String> cache = new ConcurrentHashMap<String, String>();

    private static final Pattern propertyPlaceholderRegex = Pattern.compile("\\$\\{(.*?)\\}");

    /**
     * Contains cache-keys currently being resolved - used to detect circular references.
     */
    private static final Set<String> resolvingCacheKeys = new HashSet<String>(2, 1.0f);

    /**
     * Filters {@code unfiltered} with the property values within {@code filterConsultant} and returns a copy
     * of {@code unfiltered} with the filtered value set according to {@link PropFile.Prop#with(String)}
     * @param unfiltered property to filter
     * @param uniqueIdentifier to use to cache the filtered result
     * @param filterConsultant the property values which to use to filter {@code unfiltered}
     * @return a copy of {@code unfiltered} with the proper filtered value
     * @throws Circular if {@code toFilter} contains a circular property placeholder reference
     */
    public static PropFile.Prop filter(PropFile.Prop unfiltered, String uniqueIdentifier,
                                       Map<Context, PropFileChain> filterConsultant) throws Circular {
        if ((unfiltered == null) || (uniqueIdentifier == null) || (filterConsultant == null)) {
            throw new NullPointerException(String.format("Parameters may not be null [ toFilter = %s " +
                    "| uniqueIdentifier = %s | filterConsultant = %s ].", (unfiltered == null ? "null" : "prop"),
                    uniqueIdentifier, (filterConsultant == null ? "null" : "map")));
        }
        FilterResult filterResult = _filter(unfiltered.unfilteredValue, unfiltered.context(), uniqueIdentifier, filterConsultant);
        if (Output.isDebug() && filterResult.filtered) {
            String toFilter = unfiltered.value();
            String truncatedToFilter = (toFilter.length() > 80) ? toFilter.substring(0, 80) + " [truncated]" : toFilter;
            String truncatedFiltered = (filterResult.filteredResult.length() > 80)
                    ? filterResult.filteredResult.substring(0, 80) + " [truncated]" : filterResult.filteredResult;
            Output.print("^dbug^ filtered ^b^%s^r^ to ^b^%s^r^ [ in %s%s ].", truncatedToFilter, truncatedFiltered,
                    unfiltered.context(), (Scope.Default.equals(unfiltered.scope())
                    ? "" : String.format(" with %s scope", unfiltered.scope().name)));
        }
        return unfiltered.with(filterResult.filteredResult);
    }

    /**
     * @param toFilter the value to filter
     * @param context to consult for non-context prefixed property values
     * @param uniqueIdentifier to use to cache the filtered result
     * @param filterConsultant the property values which to use to filter {@code toFilter}
     * @return the filtered value
     * @throws Circular if {@code toFilter} contains a circular property placeholder reference
     */
    public static String filter(String toFilter, Context context, String uniqueIdentifier,
                                Map<Context, PropFileChain> filterConsultant) throws Circular {
        FilterResult result = _filter(toFilter, context, uniqueIdentifier, filterConsultant);
        return result.filteredResult;
    }

    private static FilterResult _filter(String toFilter, Context context, String uniqueIdentifier,
                                        Map<Context, PropFileChain> filterConsultant) throws Circular {
        if ((toFilter == null) || (filterConsultant == null) || (context == null) || (uniqueIdentifier == null)) {
            throw new NullPointerException(String.format("Parameters may not be null [ toFilter = %s | context = %s " +
                    "| uniqueIdentifier = %s | filterConsultant = %s ].", toFilter, context, uniqueIdentifier,
                    (filterConsultant == null ? "null" : "map")));
        }
        if (!toFilter.contains("${")) {
            return new FilterResult(toFilter, false);
        }
        String cacheKey = getKey(toFilter, uniqueIdentifier);
        if (cache.containsKey(cacheKey)) {
            return new FilterResult(cache.get(cacheKey), false);
        }
        if (!resolvingCacheKeys.add(cacheKey)) {
            throw new Circular();
        }
        String filtered = toFilter;
        Matcher matcher = propertyPlaceholderRegex.matcher(toFilter);
        while (matcher.find()) {
            String propertyPlaceholder = matcher.group(1);
            // first, check the {@code context} directly
            PropFileChain chain = filterConsultant.get(context);
            if (chain != null) {
                PropFile.Prop resolved = chain.get(propertyPlaceholder);
                if (resolved != PropFile.Prop.Empty) {
                    filtered = filtered.replaceAll(Pattern.quote("${" + propertyPlaceholder + "}"),
                            Matcher.quoteReplacement(resolved.value()));
                    continue; // found!
                }
            }
            // next, parse propertyPlaceholder for a context and, if one exists, check against that
            int contextIndex = propertyPlaceholder.indexOf(".");
            if (contextIndex != -1) {
                Context contextWithinPropertyPlaceholder = Context.named(propertyPlaceholder.substring(0, contextIndex));
                String propertyPlaceholderWithoutContext = propertyPlaceholder.substring(contextIndex + 1);
                chain = filterConsultant.get(contextWithinPropertyPlaceholder);
                if (chain != null) {
                    PropFile.Prop resolved = chain.get(propertyPlaceholderWithoutContext);
                    if (resolved != PropFile.Prop.Empty) {
                        filtered = filtered.replaceAll(Pattern.quote("${" + propertyPlaceholder + "}"),
                                Matcher.quoteReplacement(resolved.value()));
                        continue; // found!
                    }
                }
            }
            // lastly, check if the property is an environment variable
            String replacement = System.getenv(propertyPlaceholder);
            if (replacement != null) {
                filtered = filtered.replaceAll(Pattern.quote("${" + propertyPlaceholder + "}"),
                        Matcher.quoteReplacement(replacement));
            } else {
                Output.print("^warn^ No filter-value found for property ^b^%s^r^", propertyPlaceholder);
            }
        }
        cache.put(cacheKey, filtered);
        resolvingCacheKeys.remove(cacheKey);
        return new FilterResult(filtered, true);
    }

    private static String getKey(String unfiltered, String uniqueIdentifier) {
        return String.format("%s#%s", unfiltered, uniqueIdentifier);
    }

    private Filter() { }

}
