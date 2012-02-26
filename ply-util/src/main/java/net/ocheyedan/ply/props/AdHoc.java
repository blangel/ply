package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: blangel
 * Date: 1/15/12
 * Time: 12:50 PM
 *
 * Provides access to ad-hoc properties passed from the command line and/or parsed from alias definitions.
 * <p/>
 * Note, within ply, ad-hoc properties are universal.  That is, even if an ad-hoc property is defined on an alias
 * definition, if that alias is encountered, the ad-hoc value becomes universally applicable to every other script
 * or alias within the execution.
 */
public final class AdHoc {

    /**
     * List of ad-hoc properties added via the command line.
     */
    static final Map<Scope, Map<Context, PropFile>> adHocProps = new ConcurrentHashMap<Scope, Map<Context, PropFile>>(3, 1.0f);

    /**
     * Parses {@code adHocProps} and adds them to the set of ad-hoc properties.
     * @param adHocProps to parse and add to the set of ad-hoc properties
     */
    public static void add(List<String> adHocProps) {
        if (adHocProps == null) {
            return;
        }
        for (String adHocProp : adHocProps) {
            parseAndAdd(adHocProp);
        }
    }

    /**
     * Creates {@link PropFile} {@link PropFile.Loc#AdHoc} objects for each distinct value within {@code system} and
     * {@code local} so that future additions via {@link #add(java.util.List)} will use these created {@link PropFile}
     * objects and thus the caller of this method will be utilizing the same objects (for instance, this is used
     * when loading so that the created {@link PropFileChain} can 'see' any ad-hoc added properties which are 
     * resolved during alias resolution.
     * @param system properties for which to create ad-hoc {@link PropFile} objects
     * @param local properties for which to created ad-hoc {@link PropFile} objects
     * @return a mapping of the created {@link PropFile} objects
     */
    static Map<Scope, Map<Context, PropFile>> produceFor(Map<Scope, Map<Context, PropFile>> system,
                                                         Map<Scope, Map<Context, PropFile>> local) {
        Map<Scope, Map<Context, PropFile>> adHocPropFiles = new ConcurrentHashMap<Scope, Map<Context, PropFile>>(3, 1.0f);
        for (Scope scope : system.keySet()) {
            Map<Context, PropFile> existingContexts = adHocProps.get(scope);
            if (existingContexts == null) {
                existingContexts = new ConcurrentHashMap<Context, PropFile>(13, 1.0f);
                adHocProps.put(scope, existingContexts);
            }
            Map<Context, PropFile> adHocContexts = new ConcurrentHashMap<Context, PropFile>(13,1.0f);
            adHocPropFiles.put(scope, adHocContexts);
            Map<Context, PropFile> contexts = system.get(scope);
            for (Context context : contexts.keySet()) {
                PropFile adHocPropFile = existingContexts.get(context);
                if (adHocPropFile == null) {
                    adHocPropFile = new PropFile(context, scope, PropFile.Loc.AdHoc);
                    existingContexts.put(context, adHocPropFile);
                }
                adHocContexts.put(context, adHocPropFile);
            }
        }
        for (Scope scope : local.keySet()) {
            Map<Context, PropFile> existingContexts = adHocProps.get(scope);
            if (existingContexts == null) {
                existingContexts = new ConcurrentHashMap<Context, PropFile>(13, 1.0f);
                adHocProps.put(scope, existingContexts);
            }
            Map<Context, PropFile> adHocContexts = adHocPropFiles.get(scope);
            if (adHocContexts == null) {
                adHocContexts = new ConcurrentHashMap<Context, PropFile>(13, 1.0f);
                adHocPropFiles.put(scope, adHocContexts);
            }
            Map<Context, PropFile> contexts = local.get(scope);
            for (Context context : contexts.keySet()) {
                PropFile adHocPropFile = existingContexts.get(context);
                if (adHocPropFile == null) {
                    adHocPropFile = new PropFile(context, scope, PropFile.Loc.AdHoc);
                    existingContexts.put(context, adHocPropFile);
                }
                if (!adHocContexts.containsKey(context)) {
                    adHocContexts.put(context, adHocPropFile);
                }
            }
        }
        // add any ad-hoc properties within {@link #adHocProps} not already added from {@code system} and {@code local}
        for (Scope scope : adHocProps.keySet()) {
            Map<Context, PropFile> contexts = adHocPropFiles.get(scope);
            if (contexts == null) {
                contexts = new ConcurrentHashMap<Context, PropFile>(12, 1.0f);
                adHocPropFiles.put(scope, contexts);
            }
            Map<Context, PropFile> existing = adHocProps.get(scope);
            for (Context context : existing.keySet()) {
                if (!contexts.containsKey(context)) {
                    contexts.put(context, existing.get(context));
                }
            }
        }
        return adHocPropFiles;
    }

    /**
     * @return all ad-hoc properties.
     */
    public static Map<Scope, Map<Context, PropFile>> get() {
        return Collections.unmodifiableMap(adHocProps);
    }

    /**
     * Parses {@code prop} which is expected to be in the format {@literal context#scope.propName=propValue}
     * where {@literal #scope} is optional.
     * @param prop to parse
     */
    static void parseAndAdd(String prop) {
        if (prop == null) {
            return;
        }
        try {
            String context, scope, propName, propValue;
            int index;
            if ((index = prop.indexOf("#")) != -1) {
                context = prop.substring(0, index);
                scope = prop.substring(index + 1, (index = prop.indexOf(".", index)));
            } else {
                context = prop.substring(0, (index = prop.indexOf(".")));
                scope = "";
            }
            propName = prop.substring(index + 1, (index = prop.indexOf("=", index)));
            propValue = prop.substring(index + 1);
            
            Context propContext = Context.named(context);
            Scope propScope = Scope.named(scope);
            Map<Context, PropFile> contexts = adHocProps.get(propScope);
            if (contexts == null) {
                contexts = new ConcurrentHashMap<Context, PropFile>(12, 1.0f);
                adHocProps.put(propScope, contexts);
            }
            PropFile adHocPropFile = contexts.get(propContext);
            if (adHocPropFile == null) {
                adHocPropFile = new PropFile(propContext, propScope, PropFile.Loc.AdHoc);
                contexts.put(propContext, adHocPropFile);
            }
            if (adHocPropFile.contains(propName)) {
                PropFile.Prop adHocProp = adHocPropFile.get(propName);
                Output.print("^warn^ Found two ad-hoc property values for ^b^%s%s.%s^r^ [ ^b^%s^r^ and ^b^%s^r^ ] using first encountered, ^b^%s^r^",
                             context, propScope.getAdHocSuffix(), propName, adHocProp.value(), propValue, adHocProp.value());
            } else {
                adHocPropFile.add(propName, propValue);
            }
        } catch (Exception e) {
            Output.print("^error^ Could not parse ad-hoc property ^b^%s^r^.", prop);
        }
    }

}