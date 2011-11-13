package net.ocheyedan.ply.exec;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.PropsExt;

import java.io.File;

/**
 * User: blangel
 * Date: 11/13/11
 * Time: 2:01 PM
 */
public final class ClojureExec {

    /**
     * Translates {@code execution#scriptArgs[0]} into an executable statement for a JVM invocation with
     * the {@literal scripts-clj.clojure.home} jar being the driver of the clj script.
     * The whole command array needs to be processed as parameters to the JVM need to be inserted
     * into the command array.
     * @param execution to invoke
     * @param projectConfigDir the ply configuration directory from which to resolve properties
     * @return the translated execution
     */
    public static Execution createClojureExecutable(Execution execution, File projectConfigDir) {
        String clojureJar = PropsExt.getValue(projectConfigDir, "scripts-clj", execution.scope, "clojure.home");
        if (clojureJar.isEmpty()) {
            Output.print("^error^ Cannot execute clojure script ^b^%s^r^ as the ^b^clojure.home^r^ property was not set within the ^b^scripts-clj^r^ context.", execution.script);
            System.exit(1);
        }
        Execution jarExec = JarExec.createJarExecutable(execution.with(clojureJar), projectConfigDir);
        // now augment the jar exec with the 'clojure.main' and the script passed in
        String[] args = jarExec.scriptArgs;
        String[] clojureArgs = new String[args.length + 2];
        System.arraycopy(args, 0, clojureArgs, 0, args.length);
        if (clojureArgs[args.length - 2].equals("-jar")) {
            clojureArgs[args.length - 2] = "-cp"; // replace the -jar with -cp
        }
        clojureArgs[args.length] = "clojure.main";
        clojureArgs[args.length + 1] = execution.script;
        return jarExec.with(clojureArgs);
    }

    private ClojureExec() { }

}
