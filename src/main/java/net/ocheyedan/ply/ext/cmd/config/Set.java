package net.ocheyedan.ply.ext.cmd.config;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.PropertiesFileUtil;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.cmd.ReliantCommand;
import net.ocheyedan.ply.ext.cmd.Usage;
import net.ocheyedan.ply.ext.props.Context;

import java.util.Properties;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:52 PM
 *
 * A {@link net.ocheyedan.ply.ext.cmd.Command} to set a property value within the project's configuration.
 */
public final class Set extends ReliantCommand {

    static class Opts {
        final Context context;
        final String propName;
        final String propValue;

        Opts(Context context, String propName, String propValue) {
            this.context = context;
            this.propName = propName;
            this.propValue = propValue;
        }
    }

    public Set(Args args) {
        super(args);
    }

    @Override public void run() {
        super.run();
        Opts opts = parse(args);
        if (opts == null) {
            new Usage(args).run();
            return;
        }
        String path = FileUtil.pathFromParts(PlyUtil.LOCAL_CONFIG_DIR.getPath(), opts.context.name + ".properties");
        Properties properties = PropertiesFileUtil.load(path, true);
        properties.setProperty(opts.propName, opts.propValue);
        PropertiesFileUtil.store(properties, path, true);
    }

    @SuppressWarnings("fallthrough")
    Opts parse(Args args) {
        switch (args.args.size()) {
            case 4:
                if ("in".equals(args.args.get(2)) && args.args.get(1).contains("=")) {
                    int index = args.args.get(1).indexOf("=");
                    String propName = args.args.get(1).substring(0, index);
                    String propValue = args.args.get(1).substring(index + 1);
                    return new Opts(new Context(args.args.get(3)), propName, propValue);
                } // fall-through
            default:
                return null;
        }
    }
}
