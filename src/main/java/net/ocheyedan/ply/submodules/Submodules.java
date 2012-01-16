package net.ocheyedan.ply.submodules;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.ListUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.util.*;

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
        Prop submodulesScopeProp = Props.get(Context.named("project"), "submodules.scope", configDirectory);
        Scope submodulesScope = (submodulesScopeProp == null ? Scope.Default : Scope.named(submodulesScopeProp.value));
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
        Collection<Prop> submodulesProps = Props.get(Context.named("submodules"), configDirectory, scope);
        if ((submodulesProps == null) || submodulesProps.isEmpty()) {
            return;
        }
        for (Prop submoduleProp : submodulesProps) {
            if ("exclude".equals(submoduleProp.value)) {
                continue;
            }
            String key = submoduleProp.name;
            String submoduleName = (parentName.isEmpty() ? key : FileUtil.pathFromParts(parentName, key));
            File submoduleConfigDir = FileUtil.fromParts(configDirectory.getPath(), "..", "..", key, ".ply", "config");
            String submoduleResolvedDepName = getSubmoduleResolvedDepName(submoduleConfigDir, scope);
            Submodule submodule = new Submodule(submoduleName, submoduleResolvedDepName);
            submodules.put(submoduleResolvedDepName, submodule);
            if (submoduleConfigDir.exists()) {
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
    private static List<Submodule> sortSubmodules(final Map<String, Submodule> submodules, File configDirectory, Scope scope) {
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
            Collection<Prop> depProps = Props.get(Context.named("dependencies"), submoduleConfigDir, scope);
            Set<String> deps = convertDeps(depProps, submodules);
            submoduleDepMap.put(submodule, deps);
        }
        // if submoduleA depends upon submoduleB then submoduleB goes first
        // if submoduleA is child of submoduleB then submoduleB goes first
        // if submoduleA is a child but submoduleB isn't then submoduleB goes first
        Comparator<Submodule> comparator = new Comparator<Submodule>() {
            @Override public int compare(final Submodule submoduleA, Submodule submoduleB) {
                Set<String> submoduleADeps = submoduleDepMap.get(submoduleA);
                Set<String> submoduleBDeps = submoduleDepMap.get(submoduleB);
                if ((submoduleADeps != null) && submoduleADeps.contains(submoduleB.dependencyName)) {
                    return 1;
                } else if ((submoduleBDeps != null) && submoduleBDeps.contains(submoduleA.dependencyName)) {
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
        ListUtil.sort(orderedSubmodules, comparator);
        return orderedSubmodules;
    }

    private static String getSubmoduleResolvedDepName(File submoduleConfigDir, Scope scope) {
        Context projectContext = Context.named("project");
        String namespace = Props.getValue(projectContext, "namespace", submoduleConfigDir, scope);
        String name = Props.getValue(projectContext, "name", submoduleConfigDir, scope);
        String version = Props.getValue(projectContext, "version", submoduleConfigDir, scope);
        String artifactName = Props.getValue(projectContext, "artifact.name", submoduleConfigDir, scope);
        String defaultArtifactName = name + "-" + version + "." + DependencyAtom.DEFAULT_PACKAGING;
        // don't pollute by placing artifactName explicitly even though it's the default
        if (artifactName.equals(defaultArtifactName)) {
            return namespace + ":" + name + ":" + version;
        } else {
            return namespace + ":" + name + ":" + version + ":" + artifactName;
        }
    }

    private static Set<String> convertDeps(Collection<Prop> depProps, Map<String, Submodule> submodules) {
        if ((depProps == null) || depProps.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> deps = new HashSet<String>(depProps.size());
        for (Prop depProp : depProps) {
            String dep = depProp.name + ":" + depProp.value;
            if (submodules.containsKey(dep)) {
                deps.add(dep);
            }
        }
        return deps;
    }

    private Submodules() { }

}
