package net.ocheyedan.ply.dep;

import java.util.List;
import java.util.Map;

/**
 * User: blangel
 * Date: 11/5/11
 * Time: 11:01 AM
 *
 * A struct to hold {@link RepositoryAtom} objects to be used during dependency resolution.
 */
public final class RepositoryRegistry {

    /**
     * The local repository used to store resolved dependencies.
     */
    public final RepositoryAtom localRepository;

    /**
     * List of remote repositories to search after consulting the {@link #localRepository} when resolving
     * dependencies.
     */
    public final List<RepositoryAtom> remoteRepositories;

    /**
     * A synthetic repository to consult before looking into either the {@link #localRepository} or
     * the list of {@link #remoteRepositories}.  Use a synthetic repository when adding dependencies; as none of the
     * actual repositories will hold the new dependency until it has been resolved correctly.  This is like a staging
     * repository until it has been determined that adding the new dependency will do no harm (i.e., cause a circular
     * dependency).
     */
    public final Map<DependencyAtom, List<DependencyAtom>> syntheticRepository;

    public RepositoryRegistry(RepositoryAtom localRepository, List<RepositoryAtom> remoteRepositories,
                              Map<DependencyAtom, List<DependencyAtom>> syntheticRepository) {
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
        this.syntheticRepository = syntheticRepository;
    }

    public boolean isEmpty() {
        return ((localRepository == null) && ((remoteRepositories == null) || !remoteRepositories.isEmpty())
                && ((syntheticRepository == null) || syntheticRepository.isEmpty()));
    }
}
