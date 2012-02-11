package net.ocheyedan.ply.cmd.config;

import net.ocheyedan.ply.OutputExt;
import net.ocheyedan.ply.cmd.Args;
import net.ocheyedan.ply.cmd.Command;

/**
 * User: blangel
 * Date: 2/10/12
 * Time: 6:23 PM
 * 
 * Abstract representation of all config {@link net.ocheyedan.ply.cmd.Command} implementations.
 */
abstract class Config extends Command.ProjectReliant {

    protected Config(Args args) {
        super(args);
    }

    @Override protected void runBeforeAssumptionsCheck() {
        OutputExt.init(); // dis-regard ad-hoc props and defined properties, simply setup for config
    }
}
