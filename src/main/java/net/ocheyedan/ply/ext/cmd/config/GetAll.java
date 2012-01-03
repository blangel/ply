package net.ocheyedan.ply.ext.cmd.config;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.ext.cmd.Args;
import net.ocheyedan.ply.ext.props.Context;
import net.ocheyedan.ply.ext.props.Prop;
import net.ocheyedan.ply.ext.props.Props;

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

    @Override protected Map<Context, Collection<Prop>> getProps() {
        return Props.get();
    }

    @Override protected String getSuffix(Prop prop) {
        if (prop.type == Prop.Loc.System) {
            return " ^green^*^r^";
        }
        return super.getSuffix(prop);
    }

    @Override protected String getNoContextSuffix() {
        return "";
    }

    @Override protected void printAppendix() {
        Output.print("^green^*^r^ indicates system-wide property.");
    }
}
