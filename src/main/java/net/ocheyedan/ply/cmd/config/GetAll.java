package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.props.Scope;

import java.util.Collection;
import java.util.Map;

/**
 * User: blangel
 * Date: 12/30/11
 * Time: 2:53 PM
 *
 * An extension to {@link Get} which prints all properties including system defaults.
 */
public class GetAll extends Get {

    public GetAll(Args args) {
        super(args);
    }

    @Override protected Map<Context, Collection<Prop>> getProps(Opts opts) {
        return Props.get(opts.scope);
    }

    @Override protected String getSuffix(Prop prop, Scope scope) {
        String superSuffix = super.getSuffix(prop, scope);
        if (prop.type == Prop.Loc.System) {
            return superSuffix + " ^green^*^r^";
        }
        return superSuffix;
    }

    @Override protected String getNothingMessageSuffix() {
        return "";
    }

    @Override protected void printAppendix(Scope scope) {
        super.printAppendix(scope);
        Output.print("^green^*^r^ indicates system-wide property.");
    }
}
