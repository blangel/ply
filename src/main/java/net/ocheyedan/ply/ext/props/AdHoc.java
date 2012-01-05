package net.ocheyedan.ply.ext.props;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 9:35 AM
 *
 * Provides access to ad-hoc properties passed from the command line.
 */
public final class AdHoc {

    /**
     * List of ad-hoc properties added via the command line.
     */
    static final List<Prop.All> adHocProps;
    static {
        adHocProps = new ArrayList<Prop.All>();
        // parse those passed in as environment variables from the ply runtime, if any.
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (!key.startsWith("ply$adHoc$")) {
                continue; // not a ply-ad-hoc property, skip
            }
            String adHocProp = key.substring(10) + "=" + env.get(key);
            parseAndAdd(adHocProp);
        }
    }

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
     * @return all ad-hoc properties.
     */
    public static Collection<Prop.All> get() {
        return Collections.unmodifiableList(adHocProps);
    }

    public static void merge() { // TODO - can this be shielded from external scripts and only accessible to ply itself
        merge(PlyUtil.LOCAL_CONFIG_DIR);
    }

    @SuppressWarnings("unchecked")
    static void merge(File configDirectory) {
        if (!Cache.contains(configDirectory)) {
            return; // let it be loaded on-demand
        }
        Collection<Prop.All> propsCol = Cache.get(configDirectory);
        if (!(propsCol instanceof List)) {
            throw new AssertionError("Expecting Cache.get internal representation to be a List<Prop.All>");
        }
        List<Prop.All> props = (List<Prop.All>) propsCol;
        Loader.loadAdHoc(props);
    }

    /**
     * Note, the properties are un-filtered as they will be filtered via the {@link Loader} class.
     * @param scope of the environment properties to retrieve.
     * @return the ad-hoc properties as a mapping to be placed as environment variables
     */
    public static Map<String, String> getEnvProps(Scope scope) {
        Map<String, String> envProps = new HashMap<String, String>(adHocProps.size() * 5); // assume ~5 per scope
        for (Prop.All prop : adHocProps) {
            Prop.Val val = prop.get(scope);
            envProps.put("ply$adHoc$" + prop.context + (Scope.Default.equals(scope) ? "" : "#" + scope.name)
                    + "." + prop.name, (val == null ? "" : val.value));
        }
        return envProps;
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
            Scope propScope = new Scope(scope);
            Prop.All adHoc = new Prop.All(Prop.Loc.AdHoc, propScope, new Context(context), propName, propValue);
            if (adHocProps.contains(adHoc)) {
                adHocProps.get(adHocProps.indexOf(adHoc)).set(propScope, Prop.Loc.AdHoc, propValue, propValue);
            } else {
                adHocProps.add(adHoc);
            }
        } catch (Exception e) {
            Output.print("^error^ Could not parse ad-hoc property ^b^%s^r^.", prop);
        }
    }

}
