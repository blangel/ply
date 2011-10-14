package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.moxie.ply.Output;

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

    public int getMaxMethodNameLength() {
        return maxMethodName.get();
    }

    @Override public boolean shouldRun(Description description) {
        if ((description.getMethodName() != null) && (description.getMethodName().length() > maxMethodName.get())) {
            maxMethodName.set(description.getMethodName().length());
        }
        return true;
    }

    @Override public String describe() {
        return "all tests (collecting padding information)";
    }

}
