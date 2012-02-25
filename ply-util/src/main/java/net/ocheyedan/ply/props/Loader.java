package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User: blangel
 * Date: 10/25/11
 * Time: 6:47 PM
 *
 * Loads properties files into {@link PropFileChain} objects.  This class caches loaded properties according to the
 * project's local configuration directory from which they were loaded and keeps them mapped by {@link Context}
 * and {@link Scope} to allow for easy retrieval.
 */
final class Loader {

    /**
     * A {@link java.io.FilenameFilter} for {@link java.util.Properties} files.
     */
    static final FilenameFilter PROPERTIES_FILENAME_FILTER = new FilenameFilter() {
        @Override public boolean accept(File dir, String name) {
            return name.endsWith(".properties");
        }
    };

    private static final Map<String, Map<Scope, Map<Context, PropFileChain>>> cache = new ConcurrentHashMap<String, Map<Scope, Map<Context, PropFileChain>>>(3, 1.0f);

    private static final AtomicBoolean systemCacheLoaded = new AtomicBoolean(false);
    private static final Map<Scope, Map<Context, PropFile>> systemCache = new ConcurrentHashMap<Scope, Map<Context, PropFile>>(3, 1.0f);

    /**
     * Loads the properties from {@code configurationDirectory} and chains them with the system and ad-hoc properties.
     * @param configurationDirectory from which to load local properties
     * @return a mapping of scope to a mapping of context to {@link PropFileChain} loaded from {@code configurationDirectory}
     *         augmented by any available ad-hoc and system properties.
     */
    static Map<Scope, Map<Context, PropFileChain>> load(File configurationDirectory) {
        String cacheKey = FileUtil.getCanonicalPath(configurationDirectory);
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        Map<Scope, Map<Context, PropFileChain>> loaded;
        if (shouldLoadFromEnv(configurationDirectory)) {
            loaded = loadFromEnv();
        } else {
            loaded = loadChain(configurationDirectory);
        }
        cache.put(cacheKey, loaded);
        return loaded;
    }

    private static boolean shouldLoadFromEnv(File configDirectory) {
        return ((configDirectory == PlyUtil.LOCAL_CONFIG_DIR)
                && (System.getenv("ply$ply.invoker") != null));
    }

    /**
     * Loads the properties from the environment variables.
     * @return the properties found within the environment variables
     */
    private static Map<Scope, Map<Context, PropFileChain>> loadFromEnv() {
        Map<Scope, Map<Context, PropFileChain>> props = new ConcurrentHashMap<Scope, Map<Context, PropFileChain>>(2, 1.0f);
        // from ply itself there is never a scope as it is resolved and exported as the default
        Scope scope = Props.getScope();
        Map<Context, PropFileChain> contexts = new ConcurrentHashMap<Context, PropFileChain>(13, 1.0f);
        props.put(scope, contexts);
        Map<Context, PropFile> propFiles = new ConcurrentHashMap<Context, PropFile>(13, 1.0f);
        
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (!key.startsWith("ply$")) {
                continue; // non-ply property
            }
            String propertyValue = env.get(key);
            key = key.substring(4); // strip ply$
            // extract context
            int index = key.indexOf("."); // error if -1; must have been set by Ply itself
            Context context = Context.named(key.substring(0, index));
            key = key.substring(index + 1); // error if (length == index + 1) as property name's are non-null
            PropFile propFile = propFiles.get(context);
            if (propFile == null) {
                propFile = new PropFile(context, scope, PropFile.Loc.System);
                propFiles.put(context, propFile);
            }
            propFile.add(key, propertyValue);
        }
        for (Context context : propFiles.keySet()) {
            PropFile propFile = propFiles.get(context);
            PropFileChain chain = new PropFileChain(contexts);
            chain.set(propFile, PropFile.Loc.System);
            contexts.put(context, chain);
        }
        return props;
    }

    /**
     * Chains together the {@link #systemCache} with {@code configurationDirectory} and any ad-hoc properties available.
     * @param configurationDirectory the local configuration directory from which to load {@link PropFile.Loc#Local} properties
     * @return a mapping from {@link Scope} to a mapping of {@link Context} to {@link PropFileChain} objects
     */
    private static Map<Scope, Map<Context, PropFileChain>> loadChain(File configurationDirectory) {
        if (!systemCacheLoaded.getAndSet(true)) {
            load(PlyUtil.SYSTEM_CONFIG_DIR, PropFile.Loc.System, systemCache);
        }
        Map<Scope, Map<Context, PropFile>> local = new ConcurrentHashMap<Scope, Map<Context, PropFile>>(3, 1.0f);
        load(configurationDirectory, PropFile.Loc.Local, local);

        Map<Scope, Map<Context, PropFile>> adHoc = AdHoc.produceFor(systemCache, local);

        Map<Scope, Map<Context, PropFileChain>> loaded = new ConcurrentHashMap<Scope, Map<Context, PropFileChain>>(3, 1.0f);
        chain(systemCache, PropFile.Loc.System, loaded);
        chain(local, PropFile.Loc.Local, loaded);
        chain(adHoc, PropFile.Loc.AdHoc, loaded);

        return loaded;
    }

    /**
     * Chains {@code files} with {@code chain} at the given {@code loc}.  This method should be called in the reverse
     * order from which properties will be resolved; i.e, called in this order:
     *  <pre>
     *  System
     *    |
     *    v
     *  Local
     *    |
     *    v
     *  AdHoc
     *  </pre>
     * @param files the property files at location {@code loc}
     * @param loc the location at which {@code files} were loaded
     * @param chain the existing chain mapping which will be used to chain together {@code files}
     */
    private static void chain(Map<Scope, Map<Context, PropFile>> files, PropFile.Loc loc,
                              Map<Scope, Map<Context, PropFileChain>> chain) {
        // handle default-scope up-front so it can be used as chains' defaults
        Map<Context, PropFileChain> defaultContextChain = chain.get(Scope.Default);
        if (defaultContextChain == null) {
            defaultContextChain = new ConcurrentHashMap<Context, PropFileChain>(12, 1.0f);
            chain.put(Scope.Default, defaultContextChain);
        }
        Map<Context, PropFile> defaultProps = files.get(Scope.Default);
        for (Context context : (defaultProps == null ? Collections.<Context>emptySet() : defaultProps.keySet())) {
            if (!defaultContextChain.containsKey(context)) {
                defaultContextChain.put(context, new PropFileChain(defaultContextChain));
            }
        }

        for (Scope scope : files.keySet()) {
            Map<Context, PropFileChain> chains = chain.get(scope);
            if (chains == null) {
                chains = new ConcurrentHashMap<Context, PropFileChain>(12, 1.0f);
                chain.put(scope, chains);
            }
            Map<Context, PropFile> contexts = files.get(scope);
            for (Context context : contexts.keySet()) {
                PropFileChain contextChain;
                if (chains.containsKey(context)) {
                    contextChain = chains.get(context);
                } else {
                    // this will never be the default-scoped context-chain as it is added upfront.
                    contextChain = new PropFileChain(defaultContextChain.get(context), chains);
                    chains.put(context, contextChain);
                }
                contextChain.set(contexts.get(context), loc);
            }
            // if this isn't the default scope - need to ensure all the default-scope's contexts are represented
            if (!Scope.Default.equals(scope)) {
                for (Context context : defaultContextChain.keySet()) {
                    if (contexts.containsKey(context)) {
                        continue;
                    }
                    // the context is not represented within the scope -> link the context for this scope to the default
                    PropFileChain contextChain;
                    if (chains.containsKey(context)) {
                        contextChain = chains.get(context);
                    } else {
                        contextChain = new PropFileChain(defaultContextChain.get(context), chains);
                        chains.put(context, contextChain);
                    }
                    if ((defaultProps != null) && defaultProps.containsKey(context)) {
                        contextChain.set(defaultProps.get(context), loc);
                    }
                }
            }
        }
    }

    /**
     * Loads all {@literal .properties} files from {@code configurationDirectory} and creates a {@link PropFile}
     * for the extracted context and scope (according to the file name) at {@code loc}
     * @param configurationDirectory from which to load properties files
     * @param loc at which the loading is occurring
     * @param into the map to store the loaded {@link PropFile} objects
     */
    private static void load(File configurationDirectory, PropFile.Loc loc, Map<Scope, Map<Context, PropFile>> into) {
        File[] subFiles = configurationDirectory.listFiles(PROPERTIES_FILENAME_FILTER);
        if (subFiles == null) {
            return;
        }
        for (File subFile : subFiles) {
            if (subFile.isDirectory()) {
                continue;
            }
            String fileName = subFile.getName();
            int index = fileName.lastIndexOf(".properties"); // not == -1 because of {@link #PROPERTIES_FILENAME_FILTER}
            fileName = fileName.substring(0, index);
            Context context; Scope scope = Scope.Default;
            if (fileName.lastIndexOf(".") != -1) {
                index = fileName.lastIndexOf(".");
                context = new Context(fileName.substring(0, index));
                scope = new Scope(fileName.substring(index + 1));
            } else {
                context = new Context(fileName.substring(0, index));
            }
            Map<Context, PropFile> contexts = into.get(scope);
            if (contexts == null) {
                contexts = new HashMap<Context, PropFile>(12, 1.0f);
                into.put(scope, contexts);
            }
            PropFile propFile = new PropFile(context, scope, loc);
            PropFiles.load(FileUtil.getCanonicalPath(subFile), propFile);
            contexts.put(context, propFile);
        }
    }

    private Loader() { }

}
