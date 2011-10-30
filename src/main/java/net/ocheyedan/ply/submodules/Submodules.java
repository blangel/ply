package net.ocheyedan.ply.submodules;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.PropsExt;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 10/27/11
 * Time: 3:55 PM
 *
 * Assists in operations related to submodules
 */
public final class Submodules {

    /**
     * Retrieves the {@literal submodules} from directory {@code localConfigDir} for scope {@code scope}.
     * Note, for each submodule found, this method recurs and collects any of its submodules as well.
     * @param localConfigDir location from which to retrieve submodules
     * @param scope of the submodules to retrieve
     * @return all submodules based on {@code localConfigDir}
     */
    public static Map<String, Prop> getSubmodules(File localConfigDir, String scope) {
        Map<String, Prop> submodules = new HashMap<String, Prop>();
        getSubmodules(localConfigDir, scope, "", submodules);
        return submodules;
    }
    private static void getSubmodules(File localConfigDir, String scope, String parent, Map<String, Prop> submodules) {
        Map<String, Prop> dirSubmodules = PropsExt.getPropsForScope(localConfigDir, "submodules", scope);
        if ((dirSubmodules == null) || dirSubmodules.isEmpty()) {
            return;
        }
        for (String key : dirSubmodules.keySet()) {
            Prop prop = dirSubmodules.get(key);
            String submodule = (parent.isEmpty() ? key : FileUtil.pathFromParts(parent, key));
            submodules.put(submodule, new Prop(prop.context, prop.scope, submodule, prop.value, prop.localOverride));
            File submoduleConfigDir = FileUtil.fromParts(localConfigDir.getPath(), "..", "..", key, ".ply", "config");
            if (submoduleConfigDir.exists()) {
                getSubmodules(submoduleConfigDir, scope, key, submodules);
            }
        }
    }

    /**
     * Orders {@code submodules}'s {@link net.ocheyedan.ply.props.Prop#name} by the directory's dependencies.
     * Any submodule within {@code submodules} whose {@link Prop#value} is equal to {@literal exclude} is ignored
     * and not included in the returned list.
     * @param submodules a collection of {@link net.ocheyedan.ply.props.Prop} objects which represent the submodules
     * @param scope which resolved {@code submodules} and is used in this method when resolving properties
     * @return an ordered list of {@code submodules} or an empty {@link java.util.List}.
     */
    public static List<Submodule> sortByDependencies(Collection<Prop> submodules, String scope) {
        if ((submodules == null) || submodules.isEmpty()) {
            return Collections.emptyList();
        }
        List<Submodule> orderedSubmodules = new ArrayList<Submodule>();
        final Map<String, List<String>> depMap = new HashMap<String, List<String>>(submodules.size());
        for (Prop submodule : submodules) {
            if ("exclude".equalsIgnoreCase(submodule.value)) {
                continue;
            }
            File submoduleConfigDir = FileUtil.fromParts(PlyUtil.LOCAL_PROJECT_DIR.getPath(), "..", submodule.name,
                    ".ply", "config");
            String submoduleResolvedDepName = getSubmoduleResolvedDepName(submoduleConfigDir, scope);
            orderedSubmodules.add(new Submodule(submodule.name, submoduleResolvedDepName));
            Map<String, Prop> deps = PropsExt.getPropsForScope(submoduleConfigDir, "dependencies", scope); // TODO - filter?
            List<DependencyAtom> dependencyAtoms = convertDeps((deps == null ? null : deps.values()));
            if (dependencyAtoms.isEmpty()) {
                continue;
            }
            Map<String, Prop> repos = PropsExt.getPropsForScope(submoduleConfigDir, "repositories", scope); // TODO - filter?
            List<RepositoryAtom> repositoryAtoms = convertRepos((repos == null ? null : repos.values()));
            Prop localRepoProp = PropsExt.get(submoduleConfigDir, "depmngr", scope, "localRepo");
            String filteredLocalRepo = PropsExt.filterForPly(submoduleConfigDir, localRepoProp, scope);
            RepositoryAtom localRepo = RepositoryAtom.parse(filteredLocalRepo);
            repositoryAtoms.add(0, localRepo);
            
            Properties resolved = Deps.resolveDependencies(dependencyAtoms, repositoryAtoms);
            List<String> submoduleResolvedDeps = new ArrayList<String>(resolved.size());
            submoduleResolvedDeps.addAll(resolved.stringPropertyNames());
            depMap.put(submoduleResolvedDepName, submoduleResolvedDeps);
        }
        // if submoduleA depends upon submoduleB then submoduleB goes first
        // if submoduleA is child of submoduleB then submoduleB goes first
        // if submoduleA is a child but submoduleB isn't then submoduleB goes first
        Collections.sort(orderedSubmodules, new Comparator<Submodule>() {
            @Override public int compare(Submodule submoduleA, Submodule submoduleB) {
                List<String> submoduleADeps = depMap.get(submoduleA.dependencyName);
                List<String> submoduleBDeps = depMap.get(submoduleB.dependencyName);
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
                return 0;
            }
        });
        return orderedSubmodules;
    }

    private static String getSubmoduleResolvedDepName(File submoduleConfigDir, String scope) {
        String namespace = PropsExt.filterForPly(submoduleConfigDir, PropsExt.get(submoduleConfigDir, "project", scope, "namespace"), scope);
        String name = PropsExt.filterForPly(submoduleConfigDir, PropsExt.get(submoduleConfigDir, "project", scope, "name"), scope);
        String version = PropsExt.filterForPly(submoduleConfigDir, PropsExt.get(submoduleConfigDir, "project", scope, "version"), scope);
        String artifactName = PropsExt.filterForPly(submoduleConfigDir, PropsExt.get(submoduleConfigDir, "project", scope, "artifact.name"), scope);
        String defaultArtifactName = name + "-" + version + "." + DependencyAtom.DEFAULT_PACKAGING;
        // don't pollute by placing artifactName explicitly even though it's the default
        if (artifactName.equals(defaultArtifactName)) {
            return namespace + ":" + name + ":" + version;
        } else {
            return namespace + ":" + name + ":" + version + ":" + artifactName;
        }
    }

    private static List<DependencyAtom> convertDeps(Collection<Prop> deps) {
        if ((deps == null) || deps.isEmpty()) {
            return Collections.emptyList();
        }
        List<DependencyAtom> dependencyAtoms = new ArrayList<DependencyAtom>();
        for (Prop dep : deps) {
            DependencyAtom dependencyAtom = DependencyAtom.parse(dep.name + ":" + dep.value, null);
            if (dependencyAtom != null) {
                dependencyAtoms.add(dependencyAtom);
            }
        }
        return dependencyAtoms;
    }

    private static List<RepositoryAtom> convertRepos(Collection<Prop> repos) {
        if ((repos == null) || repos.isEmpty()) {
            return Collections.emptyList();
        }
        List<RepositoryAtom> repositoryAtoms = new ArrayList<RepositoryAtom>();
        for (Prop repo : repos) {
            RepositoryAtom repositoryAtom = RepositoryAtom.parse(repo.value + ":" + repo.name);
            if (repositoryAtom != null) {
                repositoryAtoms.add(repositoryAtom);
            }
        }
        return repositoryAtoms;
    }

    private Submodules() { }

}
