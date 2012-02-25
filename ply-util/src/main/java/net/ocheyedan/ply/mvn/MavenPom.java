package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.props.PropFile;

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

    public final PropFile dependencies;

    public final PropFile testDependencies;

    public final PropFile modules;

    public final PropFile repositories;

    public final String buildDirectory;

    public final String buildOutputDirectory;

    public final String buildFinalName;

    public final String buildSourceDirectory;

    public final String buildTestOutputDirectory;

    public final String buildTestSourceDirectory;

    public MavenPom(String groupId, String artifactId, String version, String packaging, PropFile dependencies,
                    PropFile testDependencies, PropFile repositories, PropFile modules, String buildDirectory,
                    String buildOutputDirectory, String buildFinalName, String buildSourceDirectory,
                    String buildTestOutputDirectory, String buildTestSourceDirectory) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.packaging = packaging;
        this.dependencies = dependencies;
        this.testDependencies = testDependencies;
        this.repositories = repositories;
        this.modules = modules;
        this.buildDirectory = buildDirectory;
        this.buildOutputDirectory = buildOutputDirectory;
        this.buildFinalName = buildFinalName;
        this.buildSourceDirectory = buildSourceDirectory;
        this.buildTestOutputDirectory = buildTestOutputDirectory;
        this.buildTestSourceDirectory = buildTestSourceDirectory;
    }
}
