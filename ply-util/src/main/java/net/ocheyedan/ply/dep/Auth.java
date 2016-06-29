package net.ocheyedan.ply.dep;

import java.util.Map;

/**
 * User: blangel
 * Date: 1/9/13
 * Time: 7:26 PM
 *
 * Authorization definition for accessing secured repositories.
 */
public interface Auth {

    /**
     * Callback for {@link net.ocheyedan.ply.dep.Auth#acquireAccess(Acquisition)}.
     */
    static interface Acquisition {
        /**
         * @param username of the user
         * @param encryptedPwd of the user
         * @return auth-token or similar for the given implementation
         */
        String getAccess(String username, String encryptedPwd);
    }

    String getUsername();

    String getEncryptedPwd();

    RepositoryAtom getRepositoryAtom();

    /**
     * @return the property value to save into the {@link net.ocheyedan.ply.props.Context} named {@literal repomngr}
     */
    String getPropertyValue();

    /**
     * @param remotePathDir path into the remote repository where the artifact is located
     * @param dependencyAtom represents the artifact for which to get its path
     * @return the path to the artifact associated with {@code dependencyAtom} within {@code remotePathDir}
     */
    String getArtifactPath(String remotePathDir, DependencyAtom dependencyAtom);

    /**
     * Note for some implementations this will delegate to {@linkplain #getPath(String, String)}
     * @param remotePathDir path into the remote repository where the dependencies file is located
     * @param name represents the dependencies file for which to get the path (typically {@literal dependencies.properties})
     * @return the path to the dependencies file found within {@code remotePathDir} on the remote repository
     */
    String getDependenciesPath(String remotePathDir, String name);

    /**
     * @param remotePathDir path into the remote repository where the {@code fileName} file is located
     * @param fileName within the repository
     * @return the path to the {@code fileName} within {@code remotePathDir} on the remote repository
     */
    String getPath(String remotePathDir, String fileName);

    /**
     * @return {@literal http} headers necessary when interacting with the remote repository
     */
    Map<String, String> getHeaders();

    /**
     * Called at creation to pre-seed any authorization token information, etc.  This may be a no-op depending
     * upon the implementation.
     * @param acquisition to be used to acquire access.
     */
    void acquireAccess(Acquisition acquisition);

}
