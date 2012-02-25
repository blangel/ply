package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.PlyUtil;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.cmd.Usage;
import net.ocheyedan.ply.props.*;

import static net.ocheyedan.ply.props.PropFile.Prop;

/**
 * User: blangel
 * Date: 1/1/12
 * Time: 3:52 PM
 *
 * A {@link net.ocheyedan.ply.cmd.Command} to append a value to a property value within the project's configuration.
 */
public class Append extends Config {

    static class Opts {
        final Scope scope;
        final Context context;
        final String propName;
        final String propValue;

        Opts(Scope scope, Context context, String propName, String propValue) {
            this.scope = scope;
            this.context = context;
            this.propName = propName;
            this.propValue = propValue;
        }
    }

    public Append(Args args) {
        super(args);
    }

    @Override protected void runAfterAssumptionsCheck() {
        Opts opts = parse(args);
        if (opts == null) {
            new Usage(args).run();
            return;
        }
        String path = FileUtil.pathFromParts(PlyUtil.LOCAL_CONFIG_DIR.getPath(), opts.context.name + opts.scope.getFileSuffix() + ".properties");
        PropFile properties = new PropFile(opts.context, opts.scope, PropFile.Loc.Local);
        PropFiles.load(path, properties, true);
        String propValue = opts.propValue;
        String existing = getExisting(opts.context, opts.propName);
        if (existing != null) {
            propValue = getFromExisting(existing, propValue);
        }
        properties.remove(opts.propName);
        properties.add(opts.propName, propValue);
        PropFiles.store(properties, path, true);
    }

    protected String getFromExisting(String existing, String addition) {
        return (existing.isEmpty() ? existing :  existing + " ") + addition;
    }

    protected String getExisting(Context context, String propName) {
        PropFileChain props = Props.get(context);
        if (props == null) {
            return null;
        }
        for (Prop prop : props.props()) {
            if (prop.name.equals(propName)) {
                return prop.unfilteredValue;
            }
        }
        return null;
    }

    @SuppressWarnings("fallthrough")
    Opts parse(Args args) {
        Scope scope = Scope.Default;
        int scopeIndex = args.args.get(0).indexOf(":");
        if (scopeIndex != -1) {
            scope = Scope.named(args.args.get(0).substring(0, scopeIndex));
        }
        switch (args.args.size()) {
            case 6:
                if ("to".equals(args.args.get(2)) && "in".equals(args.args.get(4))) {
                    String propName = args.args.get(3);
                    String propValue = args.args.get(1);
                    return new Opts(scope, new Context(args.args.get(5)), propName, propValue);
                } // fall-through
            default:
                return null;
        }
    }

}
