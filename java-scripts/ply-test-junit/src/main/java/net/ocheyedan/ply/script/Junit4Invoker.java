package net.ocheyedan.ply.script;

import net.ocheyedan.ply.FileUtil;
import net.ocheyedan.ply.Output;
import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Prop;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.script.print.PrivilegedOutput;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;

import java.util.*;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 11:33 AM
 *
 * Executes {@literal junit}-4 unit tests.
 */
public class Junit4Invoker implements Runnable {

    private static final Comparator<Class> CLASS_NAME_COMPARATOR = new Comparator<Class>() {
        @Override public int compare(Class o1, Class o2) {
            return o1.getName().compareTo(o2.getName());
        }
    };

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

        List<Class> sorted = new ArrayList<Class>(classes);
        Collections.sort(sorted, CLASS_NAME_COMPARATOR);
        Request request = Request.classes(sorted.toArray(new Class[sorted.size()]));
        if (filter != null) {
            request = request.filterWith(filter);
        }
        Result result = jUnitCore.run(request);

        int syntheticCount;
        if ((syntheticCount = countSynthetic(result)) == result.getRunCount()) {
            if (originalMatchers != null) {
                PrivilegedOutput.print("^warn^ No tests matched ^b^%s^r^", originalMatchers);
            } else {
                PrivilegedOutput.print("No tests found, nothing to test.");
            }
            return;
        }
        int runCount = result.getRunCount() - syntheticCount;
        int failCount = result.getFailureCount() - syntheticCount;

        PrivilegedOutput
                .print("\nRan ^b^%d^r^ test%s in ^b^%.3f seconds^r^ with %s%d%s^r^ failure%s and %s%d^r^ ignored.\n",
                        runCount, (runCount == 1 ? "" : "s"), (result.getRunTime() / 1000.0f),
                        (failCount > 0 ? "^red^^i^ " : "^green^"), failCount, (failCount == 0 ? "" : " "), (failCount == 1 ? "" : "s"),
                        (result.getIgnoreCount() > 0 ? "^yellow^^i^" : "^b^"), result.getIgnoreCount());

        Prop reportDirProp = Props.get(Context.named("project"), "reports.dir");
        if ((failCount > 0) && Output.isInfo() && (reportDirProp != null)) {
            PrivilegedOutput.print("^info^ For %sdetailed test report%s: ", (failCount == 1 ? "a " : ""), (failCount == 1 ? "" : "s"));
            Set<String> encountered = new HashSet<String>(result.getFailureCount());
            for (Failure failure : result.getFailures()) {
                if (Junit4RunListener.isSyntheticDescription(failure.getDescription())) {
                    continue;
                }
                String reportName = FileUtil.pathFromParts(reportDirProp.value, MavenReporter.getReportName(failure.getDescription().getClassName()));
                if (encountered.add(reportName)) {
                    PrivilegedOutput.print("^info^     ^b^less %s^r^", reportName);
                }
            }
            PrivilegedOutput.print("");
        }

        if (failCount != 0) {
            System.exit(1);
        }
    }

    private int countSynthetic(Result result) {
        int synthetic = 0;
        for (Failure failure : result.getFailures()) {
            if (Junit4RunListener.isSyntheticDescription(failure.getDescription())) {
                synthetic++;
            }
        }
        return synthetic;
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
