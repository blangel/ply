package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 10:00 PM
 *
 * Does no filtering but is used as a hook to retrieve longest method name before classes/methods are actually invoked.
 */
public class AllFilterCollectPad extends Filter {

    private final AtomicInteger maxMethodName = new AtomicInteger(0);

    private final List<Filter> intersections = new ArrayList<Filter>();

    public int getMaxMethodNameLength() {
        return maxMethodName.get();
    }

    @Override public boolean shouldRun(Description description) {
        for (Filter filter : intersections) {
            if (!filter.shouldRun(description)) {
                return false;
            }
        }
        if ((description.getMethodName() != null) && (description.getMethodName().length() > maxMethodName.get())) {
            maxMethodName.set(description.getMethodName().length());
        }
        return true;
    }

    @Override public String describe() {
        return "all tests (collecting padding information)";
    }

    @Override public Filter intersect(Filter second) {
        if (second != this) {
            intersections.add(second);
        }
        return this;
    }
}
