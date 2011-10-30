package net.ocheyedan.ply.script;

import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import net.ocheyedan.ply.script.print.PrivilegedOutput;

import java.util.Set;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 11:33 AM
 *
 * Executes {@literal junit}-4 unit tests.
 */
public class Junit4Invoker implements Runnable {

    private final Set<Class> classes;

    private final Filter filter;

    private final AllFilterCollectPad padding;

    private final String originalMatchers;

    public Junit4Invoker(Set<Class> classes, String[] matchers, String unsplitMatchers) {
        this.classes = classes;
        UnionFilter filter = null;
        if (matchers != null) {
            for (String matcher : matchers) {
                if (filter == null) {
                    filter = new UnionFilter();
                }
                filter.union(new DescriptionMatcher(matcher));
            }
        }
        this.padding = new AllFilterCollectPad();
        this.filter = (filter == null ? padding : padding.intersect(filter));
        this.originalMatchers = unsplitMatchers;
    }

    @Override public void run() {
        if (classes.size() == 0) {
            PrivilegedOutput.print("No tests found, nothing to test.");
            return;
        }

        JUnitCore jUnitCore = new JUnitCore();
        jUnitCore.addListener(new Junit4RunListener(padding));
        // TODO - allow skipping of report generation or always skip and allow override
        jUnitCore.addListener(new MavenReporter());

        Request request = Request.classes(classes.toArray(new Class[classes.size()]));
        if (filter != null) {
            request = request.filterWith(filter);
        }
        Result result = jUnitCore.run(request);

        if (allSynthetic(result)) {
            if (originalMatchers != null) {
                PrivilegedOutput.print("^warn^ No tests matched ^b^%s^r^", originalMatchers);
            } else {
                PrivilegedOutput.print("No tests found, nothing to test.");
            }
            return;
        }

        PrivilegedOutput
                .print("\nRan ^b^%d^r^ test%s in ^b^%.3f seconds^r^ with %s%d^r^ failure%s and %s%d^r^ ignored.\n",
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
