package net.ocheyedan.ply;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * User: blangel
 * Date: 4/28/12
 * Time: 3:13 PM
 *
 * Waits for a character to be sent via {@link System#in} and as soon as a character is sent, treats the first
 * argument to {@literal args} as the main-class and invokes as appropriate.
 */
public class JvmPrimer {

    /**
     * @param args [0] = main-class, rest are arguments to that main-class
     */
    public static void main(String[] args) throws Throwable {
        if ((args == null) || (args.length < 1)) {
            throw new AssertionError("Expecting a main-class in args[0].");
        }
        String mainClassName = args[0];
        String[] mainClassArgs = new String[args.length - 1];
        if (mainClassArgs.length > 0) {
            System.arraycopy(args, 1, mainClassArgs, 0, mainClassArgs.length);
        }

        // wait
        int read = System.in.read();
        if (read != 0xb) { // pipe-broken?
            System.exit(read);
        }

        // ok, got go-ahead, invoke main class
        Class<?> mainClass = Class.forName(mainClassName);
        Method method = mainClass.getMethod("main", String[].class);
        try {
            method.invoke(null, new Object[]{ mainClassArgs });
        } catch (InvocationTargetException ite) {
            throw ite.getTargetException();
        }
    }

}
