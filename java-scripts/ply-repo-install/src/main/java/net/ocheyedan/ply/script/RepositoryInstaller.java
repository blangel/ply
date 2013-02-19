package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.DependencyAtom;
import net.ocheyedan.ply.dep.Deps;
import net.ocheyedan.ply.dep.Repos;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.io.File;
import java.util.Arrays;

/**
 * User: blangel
 * Date: 10/1/11
 * Time: 12:52 PM
 *
 * If run without parameters; copies the project artifact and its dependencies property file to the local repository.
 * This script has no properties inherit to it but leverages the following properties:
 * {@literal project.build.dir}/{@literal project.artifact.name} - location and name of the artifact to copy.
 * {@literal depmngr.localRepo}/{@literal project.namespace}/{@literal project.name}/{@literal project.version} - location
 * into which to copy the project artifact and its dependencies property file.
 * {@literal ply.project.dir}/config/dependencies.properties - the dependencies file to copy
 *
 * If the artifact doesn't exist at the above location, this script silently exists.
 *
 * If run with parameters; behaves similar to {@literal mvn install-file}.  Must have at least four parameters, in order,
 * {@literal namespace} - namespace of the artifact to install into the local repository.
 * {@literal name} - name of the artifact to install into the local repository.
 * {@literal version} - version of the artifact to install into the local repository.
 * {@literal file} - file-path to the artifact to install into the local repository.
 * With the following optional parameters (note if one of the optional parameters is specified they must all be specified):
 * {@literal packaging} - of the artifact to install into the local repository (default is based on file name extension
 *                        and if none exists then 'jar')
 * {@literal classifier} - of the artifact to install into the local repository; default is none.
 * {@literal dependencies-file} - a file-path to a dependencies file associated with the artifact to be installed into
 *                                the local repository.
 */
public class RepositoryInstaller {

    public static void main(String[] args) {
        try {
            Scope scope = Scope.named(Props.get("scope", Context.named("ply")).value());
            String localRepoProp = Props.get("localRepo", Context.named("depmngr")).value();
            RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp);
            if (localRepo == null) {
                Output.print("^error^ Local repository ^b^%s^r^ doesn't exist.", localRepoProp);
                System.exit(1);
            }
            // if installing a file; must at least have '... namespace name version file'
            // optionally; '... namespace name version file packaging classifier dependencies-file'
            if (args.length > 3) {
                installFile(args, localRepo, scope);
            } else if (args.length == 0) {
                installProject(localRepo, scope);
            } else {
                Output.print("^error^ Missing parameters to install-file.");
                Output.print("^error^   expected: ^b^namespace^r^ ^b^name^r^ ^b^version^r^ ^b^file^r^ [ ^b^packaging^r^ ^b^classifier^r^ ^b^dependencies-file^r^ ]");
                Output.print("^error^   given: ^b^%s^r^", Arrays.toString(args));
                System.exit(1);
            }
        } catch (Exception e) {
            Output.print(e);
            System.exit(1);
        }
    }

    private static void installFile(String[] args, RepositoryAtom localRepo, Scope scope) {
        // TODO - use scope?! as classifier?
        String namespace = args[0];
        String name = args[1];
        String version = args[2];
        String file = args[3];
        int extensionIndex = file.lastIndexOf(".");
        String packaging = (extensionIndex != -1 ? file.substring(extensionIndex + 1) : DependencyAtom.DEFAULT_PACKAGING);
        String classifier = "";
        String dependenciesFilePath = "";
        if (args.length == 7) {
            packaging = args[4];
            classifier = args[5];
            dependenciesFilePath = args[6];
        }
        File artifact = new File(file);
        if (!artifact.exists()) {
            Output.print("^error^ File to install ^b^%s^r^ doesn't exist.", file);
            System.exit(1);
        }
        String artifactName = Deps.getArtifactName(name, version, classifier, packaging);
        DependencyAtom dependencyAtom = Deps.getDepFromParts(namespace, name, version, artifactName);
        File dependenciesFile = (dependenciesFilePath.isEmpty() ? null : new File(dependenciesFilePath));
        Output.print("^info^ Copying ^b^%s^r^ into ^b^%s^r^.", file, localRepo.getPropertyName());
        Output.print("^info^ using namespace=^b^%s^r^ | name=^b^%s^r^ | version=^b^%s^r^ | packaging=^b^%s^r^ | classifier=^b^%s^r^ | dependencies-file=^b^%s^r^",
                     namespace, name, version, packaging, classifier, dependenciesFilePath);
        if (Repos.installArtifact(artifact, dependenciesFile, dependencyAtom, localRepo)) {
            Output.print("Successfully copied ^b^%s^r^ into ^b^%s^r^.", file, localRepo.getPropertyName());
        } else {
            Output.print("^error^ failed to copy ^b^%s^r^ into ^b^%s^r^.", file, localRepo.getPropertyName());
        }
    }

    private static void installProject(RepositoryAtom localRepo, Scope scope) {
        // TODO - use scope?! as classifier?
        Repos.install(localRepo);
    }

}