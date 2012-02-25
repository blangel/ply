package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.cmd.Usage;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.PropFiles;
import net.ocheyedan.ply.props.Scope;

/**
 * User: blangel
 * Date: 1/1/12
 * Time: 3:45 PM
 *
 * A {@link net.ocheyedan.ply.cmd.Command} to remove a property from a project's configuration.
 */
public final class Remove extends Config {

    static class Opts {
        final Scope scope;
        final Context context;
        final String propName;

        Opts(Scope scope, Context context, String propName) {
            this.scope = scope;
            this.context = context;
            this.propName = propName;
        }
    }

    public Remove(Args args) {
        super(args);
    }

    @Override protected void runAfterAssumptionsCheck() {
        Opts opts = parse(args);
        if (opts == null) {
            new Usage(args).run();
            return;
        }
        String path = FileUtil.pathFromParts(PlyUtil.LOCAL_CONFIG_DIR.getPath(),
                opts.context.name + opts.scope.getFileSuffix() + ".properties");
        PropFile properties = new PropFile(opts.context, opts.scope, PropFile.Loc.Local);
        PropFiles.load(path, properties, true);
        properties.remove(opts.propName);
        if (properties.isEmpty()) {
            FileUtil.fromParts(path).delete();
        } else {
            PropFiles.store(properties, path, true);
        }
    }

    @SuppressWarnings("fallthrough")
    Opts parse(Args args) {
        Scope scope = Scope.Default;
        int scopeIndex = args.args.get(0).indexOf(":");
        if (scopeIndex != -1) {
            scope = Scope.named(args.args.get(0).substring(0, scopeIndex));
        }
        switch (args.args.size()) {
            case 4:
                if ("from".equals(args.args.get(2))) {
                    String propName = args.args.get(1);
                    return new Opts(scope, new Context(args.args.get(3)), propName);
                } // fall-through
            default:
                return null;
        }
    }

}
