package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/6/11
 * Time: 10:13 AM
 *
 * Provides access to the properties configured by the user for a given project directory.
 */
public final class Props {

    /**
     * @return a mapping of {@link Context} to {@link PropFileChain} for the current scope and the local configuration
     *         directory. The result will never be null.
     * @see #get(Scope, File)
     * @see #getScope() 
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static Map<Context, PropFileChain> get() {
        return get(getScope(), PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param scope of the properties to retrieve
     * @return a mapping of {@link Context} to {@link PropFileChain} for the given {@code scope} and the local 
     *         configuration directory. The result will never be null.
     * @see #get(Scope, File)
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static Map<Context, PropFileChain> get(Scope scope) {
        return get(scope, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param scope of the properties to retrieve
     * @param configurationDirectory the configuration directory from which to load {@link PropFile.Loc#Local}
     *                               properties
     * @return a mapping of {@link Context} to {@link PropFileChain} for the given {@code scope} and the given 
     *         {@code configurationDirectory}. The result will never be null. 
     */
    public static Map<Context, PropFileChain> get(Scope scope, File configurationDirectory) {
        Map<Scope, Map<Context, PropFileChain>> loaded = Loader.load(configurationDirectory);
        if ((loaded == null) || !loaded.containsKey(scope)) {
            if ((loaded != null) && !Scope.Default.equals(scope) && loaded.containsKey(Scope.Default)) {
                return loaded.get(Scope.Default);
            } else {
                return Collections.emptyMap();
            }
        }
        return loaded.get(scope);
    }

    /**
     * @param context of the properties to retrieve
     * @return the {@link PropFileChain} for the given {@code context} for the current scope and the local configuration
     *         directory. The result will never be null, it will be empty if not present.
     * @see #get(Context, Scope, File)
     * @see #getScope()
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static PropFileChain get(Context context) {
        return get(context, getScope(), PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @return the {@link PropFileChain} for the given {@code context}, the given {@code scope} and the local configuration
     *         directory. The result will never be null, it will be empty if not present.
     * @see #get(Context, Scope, File)
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static PropFileChain get(Context context, Scope scope) {
        return get(context, scope, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param context of the properties to retrieve
     * @param scope of the properties to retrieve
     * @param configurationDirectory the configuration directory from which to load {@link PropFile.Loc#Local}
     *                               properties
     * @return the {@link PropFileChain} for the given {@code context}, the given {@code scope} and the given
     *         {@code configurationDirectory}. The result will never be null, it will be empty if not present.
     */
    public static PropFileChain get(Context context, Scope scope, File configurationDirectory) {
        Map<Context, PropFileChain> loaded = get(scope, configurationDirectory);
        if (loaded.containsKey(context)) {
            return loaded.get(context);
        } else {
            return new PropFileChain(Collections.<Context, PropFileChain>emptyMap());
        }
    }

    /**
     * @param named of the property to retrieve
     * @param context of the property to retrieve
     * @return the {@link Prop} named {@code named} for the given {@code context}, the current scope and the local
     *         configuration directory. The result will never be null, it will be {@link Prop#Empty} if not present.
     * @see #get(String, Context, Scope, File)
     * @see #getScope() 
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static Prop get(String named, Context context) {
        return get(named, context, getScope(), PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param named of the property to retrieve
     * @param context of the property to retrieve
     * @param scope of the property to retrieve
     * @return the {@link Prop} named {@code named} for the given {@code context}, the given {@code scope} and the local
     *         configuration directory. The result will never be null, it will be {@link Prop#Empty} if not present.
     * @see #get(String, Context, Scope, File)
     * @see PlyUtil#LOCAL_CONFIG_DIR
     */
    public static Prop get(String named, Context context, Scope scope) {
        return get(named, context, scope, PlyUtil.LOCAL_CONFIG_DIR);
    }

    /**
     * @param named of the property to retrieve
     * @param context of the property to retrieve
     * @param scope of the property to retrieve
     * @param configurationDirectory the configuration directory from which to load {@link PropFile.Loc#Local}
     *                               properties
     * @return the {@link Prop} named {@code named} for the given {@code context}, the given {@code scope} and the given
     *         {@code configurationDirectory}. The result will never be null, it will be {@link Prop#Empty} if not present.
     */
    public static Prop get(String named, Context context, Scope scope, File configurationDirectory) {
        PropFileChain chain = get(context, scope, configurationDirectory);
        return chain.get(named);
    }

    /**
     * @return the value of the environment variable {@literal ply$scope} or {@link Scope#Default} if the environment
     *         variable is not set.
     */
    public static Scope getScope() {
        String scope = System.getenv("ply$ply.scope"); // cannot use Props itself as this is called internally while resolving
        return (scope == null ? Scope.Default : new Scope(scope));
    }

    /**
     * For use by {@link AdHoc} when alias resolution adds ad-hoc properties for which there never was
     * a {@link PropFileChain} object created ({@link AdHoc#produceFor(java.util.Map, java.util.Map)} was never
     * invoked).
     * @param scope associated with {@code adHocPropFile}
     * @param context associated with {@code adHocPropFile}
     * @param adHocPropFile the ad hoc properties
     */
    static void addAdHoc(Scope scope, Context context, PropFile adHocPropFile) {
        Map<Scope, Map<Context, PropFileChain>> loaded = Loader.load(PlyUtil.LOCAL_CONFIG_DIR);
        Map<Context, PropFileChain> contexts = loaded.get(scope);
        if (contexts == null) {
            contexts = new ConcurrentHashMap<Context, PropFileChain>();
            loaded.put(scope, contexts);
        }
        PropFileChain chain = contexts.get(context);
        if (chain == null) {
            chain = new PropFileChain(contexts);
            contexts.put(context, chain);
        }
        chain.set(adHocPropFile, PropFile.Loc.AdHoc); // TODO should merge?
        if (Scope.Default.equals(scope)) {
            // do the same for all other scopes.
            for (Scope otherScope : loaded.keySet()) {
                if (Scope.Default.equals(otherScope)) {
                    continue;
                }
                addAdHoc(otherScope, context, adHocPropFile);
            }
        }
    }
    
    private Props() { }

}