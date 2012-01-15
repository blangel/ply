package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.exec.Execution;
import net.ocheyedan.ply.ext.props.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 1/9/12
 * Time: 9:15 PM
 *
 * Represents a build for a particular module which is either the project itself or one of its sub-modules.
 */
final class Module {

    static List<Execution> resolve(Args args, File configDirectory) {
        return new Module(args, configDirectory).resolve();
    }

    private final Args args;

    private final File configDirectory;

    /**
     * Could resolve from {@link #configDirectory} and the {@literal project.scripts} property in the constructor
     * and not use a {@link AtomicReference} but need to do this look-up lazily as resolution of the scripts could
     * expose ad-hoc properties which redefine the {@literal project.scripts} property.  The method
     * {@link #getProjectScriptPath()} does this resolution and could be called each time, however, this variable serves
     * as a cache of the first call to that method.
     */
    private final AtomicReference<String> scriptsPath;

    Module(Args args, File configDirectory) {
        this.args = args;
        this.configDirectory = configDirectory;
        this.scriptsPath = new AtomicReference<String>(null);
    }

    List<Execution> resolve() {
        List<Script> scripts = convertArgsToScripts();
        return convertScriptsToExecutions(scripts);
    }

    /**
     * @return the converted {@link #args} to {@link Script} objects
     */
    List<Script> convertArgsToScripts() {
        List<Script> scripts = new ArrayList<Script>(args.args.size());
        for (int i = 1; i < args.args.size(); i++) { // skip the build switch for now : TODO - remove the build switch
            // extract scope and arguments to script/alias, if any
            String arg = args.args.get(i);
            Script parse = Script.parse(arg, Scope.Default);
            // resolve alias, if necessary; otherwise, add as script
            Alias alias = Alias.getAlias(configDirectory, parse.scope, parse.name);
            if (alias != null) {
                processAlias(alias, scripts);
            } else {
                processScript(parse, scripts, arg);
            }
        }
        return scripts;
    }

    List<Execution> convertScriptsToExecutions(List<Script> scripts) {
        List<Execution> executions = new ArrayList<Execution>(scripts.size());
        for (Script script : scripts) {
            executions.addAll(script.convert());
        }
        return executions;
    }

    /**
     * Determines if {@code script} exists (@see {@link #doesScriptExist(Script)}) and
     * if it does adds it to {@code scripts}.  If it doesn't exist and {@code scripts} is not empty, the {@code unparsed}
     * value is added as an argument to the last script in {@code scripts}.  If {@code script} does not exist
     * and {@code scripts} is empty then an error is printed and execution is halted.
     * @param script to process for existence
     * @param scripts the list of resolved scripts to add {@code script} to if it exists
     * @param unparsed is the un-parsed argument (which created {@code script}) to be used as an argument to the last
     *                 script value within {@code scripts} if it is determined that {@code script} does not exist.
     */
    void processScript(Script script, List<Script> scripts, String unparsed) {
        Script resolvedScript;
        if ((resolvedScript = doesScriptExist(script)) != null) {
            scripts.add(resolvedScript);
        } else if (!scripts.isEmpty()) {
            scripts.get(scripts.size() - 1).arguments.add(unparsed); // add un-parsed as argument to last script
        } else {
            Output.print("^error^ Could not find script ^b^%s^r^%s.", script.name,
                    Scope.Default.equals(script.scope) ? "" : String.format(" (in scope ^b^%s^r^)", script.scope));
            System.exit(1);
        }
    }

    /**
     * For all scripts within {@code alias} calls {@link #processScript(Script, java.util.List, String)}
     * to determine if the resolved script for {@code alias} exists or is an argument to the previous
     * script within {@code alias}'s scripts.
     * @param alias to process
     * @param scripts the list of resolved scripts to add {@code alias} to once its scripts are processed.
     */
    void processAlias(Alias alias, List<Script> scripts) {
        List<Script> aliasesProcessedScripts = new ArrayList<Script>(alias.scripts.size());
        // merge this alias's ad-hoc properties if any
        AdHoc.add(alias.adHocProps);
        AdHoc.merge();
        for (Script script : alias.scripts) {
            if (script instanceof Alias) {
                processAlias((Alias) script, aliasesProcessedScripts);
            } else {
                processScript(script, aliasesProcessedScripts, script.unparsedName);
            }
        }
        scripts.add(alias.with(aliasesProcessedScripts));
    }

    /**
     * @param script to check if it exists
     * @return null if {@code script} does not exists in either the project scripts dir or the system scripts dir or is
     *         accessible from the unix shell; otherwise a new script is returned with the location set.
     */
    Script doesScriptExist(Script script) {

        String projectScriptPath = getProjectScriptPath();
        String systemScriptPath = FileUtil.getCanonicalPath(PlyUtil.SYSTEM_SCRIPTS_DIR);
        File location;

        if ((location = FileUtil.fromParts(projectScriptPath, script.name)).exists()) {
            return script.with(location);
        } else if ((location = FileUtil.fromParts(systemScriptPath, script.name)).exists()) {
            return script.with(location);
        } // TODO - handle unix shell scripts (i.e., those surrounded with quotation marks)
        return null;
    }

    String getProjectScriptPath() {
        if (scriptsPath.get() != null) {
            return scriptsPath.get();
        }
        Collection<Prop> props = Props.get(new Context("project"), configDirectory);
        String scriptsDir = FileUtil.pathFromParts(".", "scripts");
        for (Prop prop : props) {
            if ("scripts.dir".equals(prop.name)) {
                scriptsDir = prop.value;
            }
        }
        String projectConfigPath = FileUtil.getCanonicalPath(configDirectory);
        String projectScriptPath = FileUtil.getCanonicalPath(
                FileUtil.fromParts(projectConfigPath, "..", "..", scriptsDir));
        scriptsPath.set(projectScriptPath);
        return projectScriptPath;
    }
}
