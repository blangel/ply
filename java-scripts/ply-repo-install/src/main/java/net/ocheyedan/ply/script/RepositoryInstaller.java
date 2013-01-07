package net.ocheyedan.ply.script;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.dep.Repos;
import net.ocheyedan.ply.dep.RepositoryAtom;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;

import java.io.File;

/**
 * User: blangel
 * Date: 10/1/11
 * Time: 12:52 PM
 *
 * Copies the project artifact and its dependencies property file to the local repository.
 * This script has no properties inherit to it but leverages the following properties:
 * {@literal project.build.dir}/{@literal project.artifact.name} - location and name of the artifact to copy.
 * {@literal depmngr.localRepo}/{@literal project.namespace}/{@literal project.name}/{@literal project.version} - location
 * into which to copy the project artifact and its dependencies property file.
 * {@literal ply.project.dir}/config/dependencies.properties - the dependencies file to copy
 *
 * If the artifact doesn't exist at the above location, this script silently exists.
 */
public class RepositoryInstaller {

    public static void main(String[] args) {
        String localRepoProp = Props.get("localRepo", Context.named("depmngr")).value();
        RepositoryAtom localRepo = RepositoryAtom.parse(localRepoProp);
        if (localRepo == null) {
            Output.print("^error^ Local repository ^b^%s^r^ doesn't exist.", localRepoProp);
            System.exit(1);
        }
        Repos.install(localRepo);
    }

}