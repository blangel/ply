package net.ocheyedan.ply.props;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;

import java.io.File;
import java.io.FilenameFilter;
import java.util.*;
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
            return (name.endsWith(".properties") && !name.startsWith(".")); // ignore '.' files
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

    /**
     * Invalidates cached values associated with {@code configurationDirectory}
     * @param configurationDirectory from which to invalidate properties
     */
    static void invalidateCaches(File configurationDirectory) {
        String cacheKey = FileUtil.getCanonicalPath(configurationDirectory);
        cache.remove(cacheKey);
    }

    private static boolean shouldLoadFromEnv(File configDirectory) {
        return ((configDirectory == PlyUtil.LOCAL_CONFIG_DIR)
                && (System.getenv("ply_ply.invoker") != null));
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
            if (!key.startsWith("ply_")) {
                continue; // non-ply property
            }
            String propertyValue = env.get(key);
            key = key.substring(4); // strip ply_
            // extract context (note, this is java code and so the delimiter should be a '.')
            int index = key.indexOf(".");
            if (index == -1) { // shell scripts invoke with '_' delimiters, ignore these
                continue;
            }
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

        return chain(systemCache, local, adHoc);
    }

    /**
     * Chains together the inputted properties such that {@code adHoc} are consulted first during resolution, then
     * {@code local} and finally {@code system}.
     * @param system the system properties
     * @param local the local properties
     * @param adHoc the ad-hoc properties
     * @return a mapping of {@code Scope} to a mapping from {@code Context} to the {@link PropFileChain}
     */
    private static Map<Scope, Map<Context, PropFileChain>> chain(Map<Scope, Map<Context, PropFile>> system,
                                                                 Map<Scope, Map<Context, PropFile>> local,
                                                                 Map<Scope, Map<Context, PropFile>> adHoc) {
        Map<Scope, Map<Context, PropFileChain>> chain = new ConcurrentHashMap<Scope, Map<Context, PropFileChain>>(3, 1.0f);
        Map<Context, PropFileChain> defaultScopeChain = new ConcurrentHashMap<Context, PropFileChain>(13, 1.0f);
        chain.put(Scope.Default, defaultScopeChain);

        Set<Scope> allScopes = collectScopes(system, local, adHoc);
        Set<Context> allContexts = collectContexts(system, local, adHoc);

        // first do the Scope.Default so that it can be used as the default-delegate for all other scopes
        chain(system.containsKey(Scope.Default) ? system.get(Scope.Default) : Collections.<Context, PropFile>emptyMap(),
              local.containsKey(Scope.Default) ? local.get(Scope.Default) : Collections.<Context, PropFile>emptyMap(),
              adHoc.containsKey(Scope.Default) ? adHoc.get(Scope.Default) : Collections.<Context, PropFile>emptyMap(),
              defaultScopeChain, allContexts, null);
        // now do all other scopes
        for (Scope scope : allScopes) {
            if (Scope.Default.equals(scope)) {
                continue;
            }
            Map<Context, PropFile> systemFiles = system.get(scope);
            Map<Context, PropFile> localFiles = local.get(scope);
            Map<Context, PropFile> adHocFiles = adHoc.get(scope);
            Map<Context, PropFileChain> scopedChain = new ConcurrentHashMap<Context, PropFileChain>(13, 1.0f);
            chain.put(scope, scopedChain);
            chain(systemFiles == null ? Collections.<Context, PropFile>emptyMap() : systemFiles,
                  localFiles == null ? Collections.<Context, PropFile>emptyMap() : localFiles,
                  adHocFiles == null ? Collections.<Context, PropFile>emptyMap() : adHocFiles,
                  scopedChain, allContexts, defaultScopeChain);
        }

        return chain;
    }
    
    private static void chain(Map<Context, PropFile> system, Map<Context, PropFile> local,
                              Map<Context, PropFile> adHoc, Map<Context, PropFileChain> chain,
                              Set<Context> allContexts, Map<Context, PropFileChain> defaultChain) {
        chain(system, PropFile.Loc.System, chain, allContexts, defaultChain);
        chain(local, PropFile.Loc.Local, chain, allContexts, defaultChain);
        chain(adHoc, PropFile.Loc.AdHoc, chain, allContexts, defaultChain);
    }
    
    private static void chain(Map<Context, PropFile> files, PropFile.Loc loc, Map<Context, PropFileChain> chain, 
                              Set<Context> contexts, Map<Context, PropFileChain> defaultChain) {
        for (Context context : contexts) {
            PropFileChain contextChain = chain.get(context);
            if (contextChain == null) {
                PropFileChain contextDefault = (defaultChain == null ? null : defaultChain.get(context));
                contextChain = new PropFileChain(contextDefault, chain);
                chain.put(context, contextChain);
            }
            if (files.containsKey(context)) {
                contextChain.set(files.get(context), loc);
            }
        }
    }

    /**
     * @param system the system properties
     * @param local the local properties
     * @param adHoc the ad-hoc properties
     * @return a set of all {@link Scope} objects within {@code system}, {@code local} and {@code adHoc}
     */
    private static Set<Scope> collectScopes(Map<Scope, Map<Context, PropFile>> system,
                                            Map<Scope, Map<Context, PropFile>> local,
                                            Map<Scope, Map<Context, PropFile>> adHoc) {
        Set<Scope> scopes = new HashSet<Scope>(system.keySet());
        scopes.addAll(local.keySet());
        scopes.addAll(adHoc.keySet());
        return scopes;
    }

    /**
     * @param system the system properties
     * @param local the local properties
     * @param adHoc the ad-hoc properties
     * @return a set of all {@link Context} objects within {@code system}, {@code local} and {@code adHoc}
     */
    private static Set<Context> collectContexts(Map<Scope, Map<Context, PropFile>> system,
                                                Map<Scope, Map<Context, PropFile>> local,
                                                Map<Scope, Map<Context, PropFile>> adHoc) {
        Set<Context> contexts = new HashSet<Context>();
        for (Scope scope : system.keySet()) {
            contexts.addAll(system.get(scope).keySet());
        }
        for (Scope scope : local.keySet()) {
            contexts.addAll(local.get(scope).keySet());
        }
        for (Scope scope : adHoc.keySet()) {
            contexts.addAll(adHoc.get(scope).keySet());
        }
        return contexts;
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
