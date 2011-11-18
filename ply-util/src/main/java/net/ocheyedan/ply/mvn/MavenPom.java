package net.ocheyedan.ply.mvn;

import java.util.Properties;

/**
 * User: blangel
 * Date: 11/18/11
 * Time: 8:27 AM
 *
 * Encapsulates the relevant information parsed from a {@literal maven} {@literal pom} file.
 */
public final class MavenPom {

    public final String groupId;

    public final String artifactId;

    public final String version;

    public final String packaging;

    public final Properties dependencies;

    public final Properties repositories;

    public MavenPom(String groupId, String artifactId, String version, String packaging, Properties dependencies,
                    Properties repositories) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.dependencies = dependencies;
        this.repositories = repositories;
    }
}
