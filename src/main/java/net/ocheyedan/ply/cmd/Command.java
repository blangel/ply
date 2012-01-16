package net.ocheyedan.ply.cmd;

/**
 * User: blangel
 * Date: 12/22/11
 * Time: 11:03 AM
 *
 * Represents a command given to Ply from the user via the command line.
 */
public abstract class Command implements Runnable {

    public final Args args;

    protected Command(Args args) {
        this.args = args;
    }

}