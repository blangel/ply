package org.moxie.ply.script;

import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.moxie.ply.Output;

import java.util.Set;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 11:33 AM
 *
 * Executes {@literal junit}-4 unit tests.
 */
public class Junit4Runner {

    private final Set<Class> classes;

    public Junit4Runner(Set<Class> classes) {
        this.classes = classes;
    }

    public void runTests() {
        JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new Junit4RunListener());
        // TODO - allow skipping of report generation or always skip and allow override
        jUnitCore.addListener(new MavenReporter());

        Result result = jUnitCore.run(classes.toArray(new Class[classes.size()]));
        Output.print("\nRan ^b^%d^r^ test%s in ^b^%.3f seconds^r^ with %s%d^r^ failure%s and %s%d^r^ ignored.\n",
                result.getRunCount(), (result.getRunCount() == 1 ? "" : "s"),
                (result.getRunTime() / 1000.0f), (result.getFailureCount() > 0 ? "^red^^i^" : "^green^"),
                result.getFailureCount(), (result.getFailureCount() == 1 ? "" : "s"),
                (result.getIgnoreCount() > 0 ? "^yellow^^i^" : "^b^"), result.getIgnoreCount());
        if (!result.wasSuccessful()) {
            System.exit(1);
        }
    }

}
