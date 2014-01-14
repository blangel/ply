package net.ocheyedan.ply.cmd;

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

    @Override public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (String arg : args) {
            buffer.append(arg); buffer.append(' ');
        }
        for (String adHocProp : adHocProps) {
            buffer.append(adHocProp); buffer.append(' ');
        }
        return buffer.toString();
    }
}
