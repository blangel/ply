package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/21/11
 * Time: 12:24 PM
 *
 * Extensions to {@link Props} which should only be visible/applicable for the ply application itself (not any dependent
 * scripts).
 */
public final class PropsExt {

    /**
     * A cache of project-path/scope to resolved environment properties.
     */
    private static final Map<String, Map<String, String>> RESOLVED_ENV_CACHE = new HashMap<String, Map<String, String>>();

    /**
     * From the resolved properties, creates a mapping appropriate for exporting to a process's system environment
     * variables.  The mapping returned by this method will only include the contexts' {@code scope} (and the default scope's
     * if the given {@code scope} didn't override the default scope's property).
     * The key is composed of 'ply$' (to distinguish ply variables from other system environment variables) concatenated with
     * the context concatenated with '.' and the property name (note, the scope has been discarded).
     * @param configDirectory configuration directory associated for the project.
     * @param scope of the properties to include in the environment properties mapping
     * @return a mapping of env-property-name to property value (using {@code scope})
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getPropsForEnv(File configDirectory, Scope scope) {
        String cacheKey = FileUtil.pathFromParts(FileUtil.getCanonicalPath(configDirectory), scope.name);
        if (RESOLVED_ENV_CACHE.containsKey(cacheKey)) {
            return RESOLVED_ENV_CACHE.get(cacheKey);
        }
        Map<Context, Collection<Prop>> props = Props.get(configDirectory, scope);
        Map<String, String> envProps = new HashMap<String, String>(props.size() * 5); // assume avg of 5 props per context?
        for (Context context : props.keySet()) {
            Collection<Prop> contextProps = props.get(context);
            for (Prop prop : contextProps) {
                String envKey = "ply$" + context + "." + prop.name;
                envProps.put(envKey, prop.value);
            }
        }
        // now add some synthetic properties like the local ply directory location.
        envProps.put("ply$ply.project.dir", FileUtil.getCanonicalPath(FileUtil.fromParts(configDirectory.getPath(), "..")));
        envProps.put("ply$ply.java", System.getProperty("ply.java"));
        // scripts are always executed from the '.ply/../' directory, allow them to know where the 'ply' invocation
        // actually occurred.
        envProps.put("ply$ply.original.user.dir", System.getProperty("user.dir"));
        // allow scripts access to which scope in which they are being invoked.
        envProps.put("ply$ply.scope", scope.name);
        // finally, add a property to signify that the script is being invoked via ply
        envProps.put("ply$ply.invoker", "ply");

        RESOLVED_ENV_CACHE.put(cacheKey, envProps);
        return envProps;
    }

    private PropsExt() { }

}
