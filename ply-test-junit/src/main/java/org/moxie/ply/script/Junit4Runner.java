package org.moxie.ply.script;

import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
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

    private final Filter filter;

    private final String originalMatchers;

    public Junit4Runner(Set<Class> classes) {
        this(classes, null, null);
    }

    public Junit4Runner(Set<Class> classes, String[] matchers, String unsplitMatchers) {
        this.classes = classes;
        Filter filter = null;
        if (matchers != null) {
            for (String matcher : matchers) {
                if (filter == null) {
                    filter = new DescriptionMatcher(matcher);
                } else {
                    filter.intersect(new DescriptionMatcher(matcher));
                }
            }
        }
        this.filter = filter;
        this.originalMatchers = unsplitMatchers;
    }

    public void runTests() {
        if (classes.size() == 0) {
            Output.print("No tests found, nothing to test.");
            return;
        }

        JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new Junit4RunListener());
        // TODO - allow skipping of report generation or always skip and allow override
        jUnitCore.addListener(new MavenReporter());

        Request request = Request.classes(classes.toArray(new Class[classes.size()]));
        if (filter != null) {
            request = request.filterWith(filter);
        }
        Result result = jUnitCore.run(request);

        if (allSynthetic(result)) {
            if (originalMatchers != null) {
                Output.print("^warn^ No tests matched ^b^%s^r^", originalMatchers);
            } else {
                Output.print("No tests found, nothing to test.");
            }
            return;
        }

        Output.print("\nRan ^b^%d^r^ test%s in ^b^%.3f seconds^r^ with %s%d^r^ failure%s and %s%d^r^ ignored.\n",
                result.getRunCount(), (result.getRunCount() == 1 ? "" : "s"),
                (result.getRunTime() / 1000.0f), (result.getFailureCount() > 0 ? "^red^^i^" : "^green^"),
                result.getFailureCount(), (result.getFailureCount() == 1 ? "" : "s"),
                (result.getIgnoreCount() > 0 ? "^yellow^^i^" : "^b^"), result.getIgnoreCount());

        if (!result.wasSuccessful()) {
            System.exit(1);
        }
    }

    private boolean allSynthetic(Result result) {
        for (Failure failure : result.getFailures()) {
            if (!Junit4RunListener.isSyntheticDescription(failure.getDescription())) {
                return false;
            }
        }
        return (result.getFailureCount() > 0);
    }

}
