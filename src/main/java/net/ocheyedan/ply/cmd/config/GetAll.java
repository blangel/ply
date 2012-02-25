package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.props.PropFile;
import net.ocheyedan.ply.props.Scope;

import static net.ocheyedan.ply.props.PropFile.Prop;

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

    @Override protected boolean accept(Prop prop) {
        return true;
    }

    @Override protected String getSuffix(Prop prop, Scope scope) {
        String superSuffix = super.getSuffix(prop, scope);
        if (prop.loc() == PropFile.Loc.System) {
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
