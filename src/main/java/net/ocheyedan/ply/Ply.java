package net.ocheyedan.ply;

import net.ocheyedan.ply.cmd.Command;
import net.ocheyedan.ply.cmd.CommandLineParser;
import net.ocheyedan.ply.props.AdHoc;

/**
 * User: blangel
 * Date: 9/2/11
 * Time: 11:03 AM
 *
 * The main entry-point, invoked via a {@literal Unix} style command line.
 */
public final class Ply {

    public static void main(String[] args) {

        try {
            SystemExit.ply = true;
            Command command = CommandLineParser.parse(args);
            AdHoc.add(command.args.adHocProps);
            command.run();
        } catch (SystemExit se) {
            Output.init(); // ensure the queue-ed messages have been printed
            System.exit(se.exitCode);
        }

    }

}