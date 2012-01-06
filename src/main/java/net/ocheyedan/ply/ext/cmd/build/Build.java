package net.ocheyedan.ply.ext.cmd.build;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.Command;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import net.ocheyedan.ply.ext.props.Props;
import net.ocheyedan.ply.ext.props.Scope;

import java.io.File;
import java.util.*;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:50 PM
 *
 * A {@link net.ocheyedan.ply.ext.cmd.Command} to handle running build scripts.
 * This is where arguments are translated to scripts and resolved against aliases.  After this translation,
 * the scripts are turned into executions and invoked.
 */
public final class Build extends Command {

    public Build(Args args) {
        super(args);
    }

    public void run() {
        List<Script> scripts = convertArgsToScripts();
        print(scripts, "");
    }

    private void print(List<Script> scripts, String prefix) {
        for (Script script : scripts) {
            if (script instanceof Alias) {
                Output.print("%sscripts from alias ^b^%s^r^ (scope = %s)%s:", prefix, script.name, script.scope.name,
                        script.arguments.isEmpty() ? "" : String.format(" (args = %s)", script.arguments.toString()));
                print(((Alias) script).scripts, prefix + "  ");
            } else {
                Output.print("%s^b^%s^r^ (scope = %s)%s", prefix, script.name, script.scope.name,
                        script.arguments.isEmpty() ? "" : String.format(" (args = %s)", script.arguments.toString()));
            }
        }
    }

    protected List<Script> convertArgsToScripts() {
        File scriptDir = getScriptDir();
        File systemScriptDir = FileUtil.fromParts(PlyUtil.INSTALL_DIRECTORY, "scripts");
        List<Script> scripts = new ArrayList<Script>(args.args.size());
        for (int i = 1; i < args.args.size(); i++) { // skip the build switch for now : TODO - remove the build switch
            // extract scope and arguments to script/alias, if any
            String arg = args.args.get(i);
            Script parse = Script.parse(arg, Scope.Default);
            // resolve alias, if necessary; otherwise, add as script
            Alias alias = Alias.getAlias(parse.scope, parse.name);
            if (alias != null) {
                processAlias(alias, scriptDir, systemScriptDir, scripts);
            } else {
                processScript(parse, scriptDir, systemScriptDir, scripts, arg);
            }
        }
        return scripts;
    }

    /**
     * Determines if {@code script} exists (@see {@link #doesScriptExist(Script, java.io.File, java.io.File)}) and
     * if it does adds it to {@code scripts}.  If it doesn't exist and {@code scripts} is not empty, the {@code unparsed}
     * value is added as an argument to the last script in {@code scripts}.  If {@code script} does not exist
     * and {@code scripts} is empty then an error is printed and execution is halted.
     * @param script to process for existence
     * @param projectScriptDir used in checking existence of {@code script}
     * @param systemScriptDir used in checking existence of {@code script}
     * @param scripts the list of resolved scripts to add {@code script} to if it exists
     * @param unparsed is the un-parsed argument (which created {@code script}) to be used as an argument to the last
     *                 script value within {@code scripts} if it is determined that {@code script} does not exist.
     */
    protected void processScript(Script script, File projectScriptDir, File systemScriptDir, List<Script> scripts,
                                 String unparsed) {
        if (doesScriptExist(script, projectScriptDir, systemScriptDir) == null) {
            scripts.add(script);
        } else if (!scripts.isEmpty()) {
            scripts.get(scripts.size() - 1).arguments.add(unparsed); // add un-parsed as argument to last script
        } else {
            Output.print("^error^ Could not find script ^b^%s^r^%s.", script.name,
                    Scope.Default.equals(script.scope) ? "" : String.format(" (in scope ^b^%s^r^)", script.scope));
            System.exit(1);
        }
    }

    /**
     * For all scripts within {@code alias} calls {@link #processScript(Script, java.io.File, java.io.File, java.util.List, String)}
     * to determine if the resolved script for {@code alias} exists or is an argument to the previous
     * script within {@code alias}'s scripts.
     * @param alias to process
     * @param projectScriptDir used in checking script existence
     * @param systemScriptDir used in checking script existence
     * @param scripts the list of resolved scripts to add {@code alias} to once its scripts are processed.
     */
    protected void processAlias(Alias alias, File projectScriptDir, File systemScriptDir, List<Script> scripts) {
        List<Script> aliasesProcessedScripts = new ArrayList<Script>(alias.scripts.size());
        for (Script script : alias.scripts) {
            if (script instanceof Alias) {
                processAlias((Alias) script, projectScriptDir, systemScriptDir, aliasesProcessedScripts);
            } else {
                processScript(script, projectScriptDir, systemScriptDir, aliasesProcessedScripts, script.unparsedName);
            }
        }
        scripts.add(alias.with(aliasesProcessedScripts));
    }

    /**
     * @param script to check if it exists
     * @param projectScriptDir the local scripts directory
     * @param systemScriptDir the system defined scripts directory
     * @return null if {@code script} exists in either {@code projectScriptDir} or {@code systemScriptDir} or is
     *         accessible from the unix shell; otherwise the script name is returned.
     */
    protected String doesScriptExist(Script script, File projectScriptDir, File systemScriptDir) {
        if (FileUtil.fromParts(projectScriptDir.getPath(), script.name).exists()) {
            return null;
        } else if (FileUtil.fromParts(systemScriptDir.getPath(), script.name).exists()) {
            return null;
        } // TODO - handle unix shell scripts (i.e., those surrounded with quotation marks)
        return script.name;
    }

    protected File getScriptDir() {
        Collection<Prop> props = Props.get(new Context("project"));
        String scriptsDir = FileUtil.pathFromParts(".", "scripts");
        for (Prop prop : props) {
            if ("scripts.dir".equals(prop.name)) {
                scriptsDir = prop.value;
            }
        }
        return new File(scriptsDir);
    }

}
