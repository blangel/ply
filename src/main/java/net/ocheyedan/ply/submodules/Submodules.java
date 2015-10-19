package net.ocheyedan.ply.submodules;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFileChain;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.util.*;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 10/27/11
 * Time: 3:55 PM
 *
 * Assists in operations related to {@link Submodule} objects.
 */
public final class Submodules {

    /**
     * Retrieves the {@literal submodules} from directory {@code configDirectory} and then orders the results based on
     * the following scheme:
     * If submoduleA depends upon submoduleB then submoduleB goes first;
     * else if submoduleA is child of submoduleB then submoduleB goes first;
     * else if submoduleA is a child but submoduleB isn't then submoduleB goes first;
     * else submoduleA is equal to submoduleB
     *
     * Note, for each submodule found, this method recurs and collects any of its submodules as well.
     * Any submodule whose {@link Prop#value} is equal to {@literal exclude} is ignored and not included in the returned
     * map.
     * @param configDirectory location from which to retrieve submodules
     * @return all {@link Submodule} based on {@code localConfigDir} mapped to their own {@link Submodule} objects.
     */
    public static List<Submodule> getSubmodules(File configDirectory) {
        Prop submodulesScopeProp = Props.get("submodules.scope", Context.named("project"), Props.getScope(), configDirectory);
        Scope submodulesScope = (submodulesScopeProp == null ? Scope.Default : Scope.named(submodulesScopeProp.value()));
        Map<String, Submodule> submodules = new HashMap<String, Submodule>();
        getSubmodules(configDirectory, submodulesScope, "", submodules);
        return sortSubmodules(submodules, configDirectory, submodulesScope);
    }

    /**
     * Retrieves the {@literal submodules} from directory {@code configDirectory} for scope {@code scope}.
     * Note, for each submodule found, this method recurs and collects any of its submodules as well.
     * Any submodule whose {@link Prop#value} is equal to {@literal exclude} is ignored and not included in the given
     * map.
     * @param configDirectory location from which to retrieve submodules
     * @param scope of the submodules to retrieve
     * @param parentName is the parent submodule name for the invocation (relative)
     * @param submodules all {@link Submodule} based on {@code localConfigDir} mapped by their dependency name.
     */
    private static void getSubmodules(File configDirectory, Scope scope, String parentName,
                                      Map<String, Submodule> submodules) {
        PropFileChain submodulesProps = Props.get(Context.named("submodules"), scope, configDirectory);
        if (submodulesProps == null) {
            return;
        }
        for (Prop submoduleProp : submodulesProps.props()) {
            if ("exclude".equals(submoduleProp.value())) {
                continue;
            }
            String key = submoduleProp.name;
            String submoduleName = (parentName.isEmpty() ? key : FileUtil.pathFromParts(parentName, key));
            File submoduleConfigDir = FileUtil.fromParts(configDirectory.getPath(), "..", "..", key, ".ply", "config");
            if (submoduleConfigDir.exists()) {
                String submoduleResolvedDepName = getSubmoduleResolvedDepName(submoduleConfigDir, scope);
                Submodule submodule = new Submodule(submoduleName, submoduleResolvedDepName);
                submodules.put(submoduleResolvedDepName, submodule);
                getSubmodules(submoduleConfigDir, scope, key, submodules);
            }
        }
    }

    /**
     * Sorts {@code submodules} by the following:
     * If submoduleA depends upon submoduleB then submoduleB goes first;
     * else if submoduleA is child of submoduleB then submoduleB goes first;
     * else if submoduleA is a child but submoduleB isn't then submoduleB goes first;
     * else submoduleA is equal to submoduleB

     * @param submodules which to sort; {@link Submodule} objects mapped by their dependency name.
     * @param configDirectory the configuration directory of the project from which the {@code submodules} originated
     * @param scope of the retrieved {@code submodules}
     * @return the sorted list of {@code submodules}
     */
    static List<Submodule> sortSubmodules(final Map<String, Submodule> submodules, File configDirectory, Scope scope) {
        if ((submodules == null) || submodules.isEmpty()) {
            return Collections.emptyList();
        }
        List<Submodule> orderedSubmodules = new ArrayList<Submodule>();
        final Map<Submodule, Set<String>> submoduleDepMap = new HashMap<Submodule, Set<String>>();
        for (String submoduleDepName : submodules.keySet()) {
            Submodule submodule = submodules.get(submoduleDepName);
            orderedSubmodules.add(submodule);
            File submoduleConfigDir = FileUtil.fromParts(FileUtil.getCanonicalPath(configDirectory), "..", "..",
                                                         submodule.name, ".ply", "config");
            PropFileChain depProps = Props.get(Context.named("dependencies"), scope, submoduleConfigDir);
            Set<String> deps = convertDeps(depProps, submodules);
            submoduleDepMap.put(submodule, deps);
        }
        // if submoduleA depends upon submoduleB then submoduleB goes first
        // if submoduleA is child of submoduleB then submoduleB goes first
        // if submoduleA is a child but submoduleB isn't then submoduleB goes first
        Comparator<Submodule> comparator = new Comparator<Submodule>() {
            @Override public int compare(final Submodule submoduleA, Submodule submoduleB) {
                if (submoduleA.name.equals(submoduleB.name)) {
                    return 0;
                }
                if (dependsUpon(submoduleA, submoduleB, submodules, submoduleDepMap)) {
                    return 1;
                } else if (dependsUpon(submoduleB, submoduleA, submodules, submoduleDepMap)) {
                    return -1;
                }
                if (submoduleA.name.contains(submoduleB.name)) {
                    return 1;
                } else if (submoduleB.name.contains(submoduleA.name)) {
                    return -1;
                }
                if (submoduleA.name.contains(File.separator) && !submoduleB.name.contains(File.separator)) {
                    return 1;
                } else if (submoduleB.name.contains(File.separator) && !submoduleA.name.contains(File.separator)) {
                    return -1;
                }
                return 0; // TODO - default to order specified by user
            }
        };
        Collections.sort(orderedSubmodules, comparator);
        return orderedSubmodules;
    }

    /**
     * @param submodule to see if it depends upon {@code dependencyToCheck}
     * @param dependencyToCheck whether it is a dependency of {@code submodule}
     * @param submodules a mapping of a submodules dependency name to the actual {@link Submodule} object
     * @param submoduleDepMap a mapping of {@link Submodule} to its dependencies' names
     * @return true if {@code submodule} depends upon {@code dependencyToCheck}
     */
    private static boolean dependsUpon(Submodule submodule, Submodule dependencyToCheck, Map<String, Submodule> submodules,
                                       Map<Submodule, Set<String>> submoduleDepMap) {
        Set<String> submoduleDeps = submoduleDepMap.get(submodule);
        if (submoduleDeps == null) {
            return false;
        }
        if (submoduleDeps.contains(dependencyToCheck.dependencyName)) {
            return true;
        }
        for (String dep : submoduleDeps) {
            if (submodules.containsKey(dep) && dependsUpon(submodules.get(dep), dependencyToCheck,
                                                           submodules, submoduleDepMap)) {
                return true;
            }
        }
        return false;
    }

    private static String getSubmoduleResolvedDepName(File submoduleConfigDir, Scope scope) {
        Context projectContext = Context.named("project");
        String namespace = Props.get("namespace", projectContext, scope, submoduleConfigDir).value();
        String name = Props.get("name", projectContext, scope, submoduleConfigDir).value();
        String version = Props.get("version", projectContext, scope, submoduleConfigDir).value();
        String artifactName = Props.get("artifact.name", projectContext, scope, submoduleConfigDir).value();
        String defaultArtifactName = name + "-" + version + "." + DependencyAtom.DEFAULT_PACKAGING;
        // don't pollute by placing artifactName explicitly even though it's the default
        if (artifactName.equals(defaultArtifactName)) {
            return namespace + ":" + name + ":" + version;
        } else {
            return namespace + ":" + name + ":" + version + ":" + artifactName;
        }
    }

    private static Set<String> convertDeps(PropFileChain depProps, Map<String, Submodule> submodules) {
        if (depProps == null) {
            return Collections.emptySet();
        }
        Set<String> deps = new HashSet<String>();
        for (Prop depProp : depProps.props()) {
            String dep = depProp.name + ":" + depProp.value();
            if (submodules.containsKey(dep)) {
                deps.add(dep);
            }
        }
        return deps;
    }

    private Submodules() { }

}
