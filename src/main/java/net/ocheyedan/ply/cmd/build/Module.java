package net.ocheyedan.ply.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.SystemExit;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.exec.Execution;
import net.ocheyedan.ply.props.*;

import java.io.File;
import java.util.ArrayList;
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
        // resolve ad-hoc properties before creating executions (as execution may depend upon a value of an ad-hoc prop).
        // must invalidate filtered-cache (as ad-hoc props may have changed the filtered values).
        if (handleAdHoc(scripts)) {
            PropsExt.invalidateFilteredCaches(configDirectory);
        }
        // now apply resolved ad-hoc properties to scripts themselves
        List<Script> filtered = new ArrayList<Script>(scripts.size());
        for (Script script : scripts) {
            filtered.add(script.filter());
        }
        // now that all ad-hoc props are accounted for and filtered, convert scripts to executions
        return convertScriptsToExecutions(filtered);
    }
    
    private boolean handleAdHoc(List<Script> scripts) {
        boolean hasAdHocPropsFromAliases = false;
        for (Script script : scripts) {
            if (script instanceof Alias) {
                Alias alias = (Alias) script;
                List<String> adHocProps = alias.adHocProps;
                hasAdHocPropsFromAliases = (hasAdHocPropsFromAliases || handleAdHoc(alias.scripts));
                if ((adHocProps != null) && !adHocProps.isEmpty()) {
                    AdHoc.add(adHocProps);
                    hasAdHocPropsFromAliases = true;
                }
            }
        }
        return hasAdHocPropsFromAliases;
    }

    /**
     * @return the converted {@link #args} to {@link Script} objects
     */
    List<Script> convertArgsToScripts() {
        List<Script> scripts = new ArrayList<Script>(args.args.size());
        for (String arg : args.args) {
            // extract scope and arguments to script/alias, if any
            Script parse = Script.parse(arg, Scope.Default);
            // resolve alias, if necessary; otherwise, add as script
            Alias alias = Alias.getAlias(configDirectory, parse.scope, parse.name);
            if (alias != null) {
                processAlias(alias.augment(parse.arguments, parse.unparsedName), scripts);
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
            Output.print("^info^    Place ^b^%s^r^ script into the project's scripts directory (current scripts' directory: ^b^ply %sget-all scripts.dir from project^r^).", script.name, script.scope.getScriptPrefix());
            Output.print("^info^    Or make ^b^%s^r^ an alias (^b^ply set %s%s=xxxx in aliases^r^).", script.name, script.scope.getScriptPrefix(), script.name);
            throw new SystemExit(1);
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
        } else if (script.name.startsWith("`") && script.name.endsWith("`")) {
            return new ShellScript(script);
        }
        return null;
    }

    String getProjectScriptPath() {
        if (scriptsPath.get() != null) {
            return scriptsPath.get();
        }
        String projectScriptsDir = Props.get("scripts.dir", Context.named("project"), Props.getScope(), configDirectory).value();
        String scriptsDir = (projectScriptsDir.isEmpty() ? FileUtil.pathFromParts(".", "scripts") : projectScriptsDir);
        String projectConfigPath = FileUtil.getCanonicalPath(configDirectory);
        String projectScriptPath = FileUtil.getCanonicalPath(
                FileUtil.fromParts(projectConfigPath, "..", "..", scriptsDir));
        scriptsPath.set(projectScriptPath);
        return projectScriptPath;
    }
}
