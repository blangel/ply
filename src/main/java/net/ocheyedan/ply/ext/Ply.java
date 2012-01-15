package net.ocheyedan.ply.ext;

import net.ocheyedan.ply.ext.cmd.Command;
import net.ocheyedan.ply.ext.cmd.CommandLineParser;
import net.ocheyedan.ply.ext.props.AdHoc;

/**
 * User: blangel
 * Date: 12/22/11
 * Time: 11:03 AM
 *
 * The main entry-point, invoked via a {@literal Unix} style command line.
 */
public final class Ply {

    public static void main(String[] args) {

        Command command = CommandLineParser.parse(args);
        AdHoc.add(command.args.adHocProps);
        command.run();

    }

}