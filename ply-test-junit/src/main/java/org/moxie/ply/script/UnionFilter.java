package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.ArrayList;
import java.util.List;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 9:43 PM
 */
public class UnionFilter extends Filter {

    private final List<Filter> filters = new ArrayList<Filter>();

    public void union(Filter filter) {
        filters.add(filter);
    }

    @Override public boolean shouldRun(Description description) {
        for (Filter filter : filters) {
            if (filter.shouldRun(description)) {
                return true;
            }
        }
        return false;
    }

    @Override public String describe() {
        return "union filter";
    }

    @Override public Filter intersect(Filter second) {
        throw new UnsupportedOperationException("This is a union filter");
    }
}
