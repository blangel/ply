package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.OutputExt;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.cmd.ReliantCommand;
import net.ocheyedan.ply.cmd.Usage;
import net.ocheyedan.ply.props.Context;

import java.util.Properties;

/**
 * User: blangel
 * Date: 1/1/12
 * Time: 3:45 PM
 *
 * A {@link net.ocheyedan.ply.cmd.Command} to remove a property from a project's configuration.
 */
public final class Remove extends ReliantCommand {

    static class Opts {
        final Context context;
        final String propName;

        Opts(Context context, String propName) {
            this.context = context;
            this.propName = propName;
        }
    }

    public Remove(Args args) {
        super(args);
    }

    @Override public void run() {
        OutputExt.init(); // dis-regard ad-hoc props and defined properties, simply init
        super.run();
        Opts opts = parse(args);
        if (opts == null) {
            new Usage(args).run();
            return;
        }
        String path = FileUtil.pathFromParts(PlyUtil.LOCAL_CONFIG_DIR.getPath(), opts.context.name + ".properties");
        Properties properties = PropertiesFileUtil.load(path, true);
        properties.remove(opts.propName);
        if (properties.isEmpty()) {
            FileUtil.fromParts(path).delete();
        } else {
            PropertiesFileUtil.store(properties, path, true);
        }
    }

    @SuppressWarnings("fallthrough")
    Opts parse(Args args) {
        switch (args.args.size()) {
            case 4:
                if ("from".equals(args.args.get(2))) {
                    String propName = args.args.get(1);
                    return new Opts(new Context(args.args.get(3)), propName);
                } // fall-through
            default:
                return null;
        }
    }

}
