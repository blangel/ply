package net.ocheyedan.ply.ext.cmd;

import java.util.List;

/**
 * User: blangel
 * Date: 12/28/11
 * Time: 4:39 PM
 *
 * Represents the command line arguments passed to ply.
 */
public final class Args {

    public final List<String> args;

    public final List<String> adHocProps;

    public Args(List<String> args, List<String> adHocProps) {
        this.args = args;
        this.adHocProps = adHocProps;
    }
}
