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
                Output.print("%sscripts from alias ^b^%s^r^ (scope = %s):", prefix, script.name, script.scope.name);
                print(((Alias) script).scripts, prefix + "  ");
            } else {
                Output.print("%s^b^%s^r^ (scope = %s)", prefix, script.name, script.scope.name);
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
                String exists;
                if ((exists = doesAliasExist(alias, scriptDir, systemScriptDir)) == null) {
                    scripts.add(alias);
                } else {
                    Output.print("^error^ Could not find scripts defined by ^b^%s^r^%s.", exists,
                            Scope.Default.equals(parse.scope) ? "" : String.format(" (in scope ^b^%s^r^)", parse.scope));
                    System.exit(1);
                }
            } else {
                if (doesScriptExist(parse, scriptDir, systemScriptDir) == null) {
                    scripts.add(parse);
                } else {
                    Output.print("^error^ Could not find script ^b^%s^r^%s.", parse.name,
                            Scope.Default.equals(parse.scope) ? "" : String.format(" (in scope ^b^%s^r^)", parse.scope));
                    System.exit(1);
                }
            }
        }
        return scripts;
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

    /**
     * @param alias to check if all of its scripts exist
     * @param projectScriptDir the local scripts directory
     * @param systemScriptDir the system defined scripts directory
     * @return null if all of {@code alias}'s scripts exists in either {@code projectScriptDir} or
     *         {@code systemScriptDir} or are accessible from the unix shell; otherwise the offending alias name is
     *         returned.
     */
    protected String doesAliasExist(Alias alias, File projectScriptDir, File systemScriptDir) {
        for (Script script : alias.scripts) {
            if (script instanceof Alias) {
                if (doesAliasExist((Alias) script, projectScriptDir, systemScriptDir) != null) {
                    return script.name;
                }
            } else if (doesScriptExist(script, projectScriptDir, systemScriptDir) != null) {
                return alias.name;
            }
        }
        return null;
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
