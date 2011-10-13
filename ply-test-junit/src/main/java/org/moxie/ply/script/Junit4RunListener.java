package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.moxie.ply.Output;

import java.util.HashMap;
import java.util.Map;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 1:35 PM
 */
public class Junit4RunListener extends RunListener {

    final Map<Description, Failure> failures = new HashMap<Description, Failure>();

    final Map<String, Integer> methodNameOffsets = new HashMap<String, Integer>();

    @Override public void testRunStarted(Description description) throws Exception {
        Output.print("\nRunning tests from ^b^%d^r^ classes\n", description.getChildren().size());
    }

    @Override public void testStarted(Description description) throws Exception {
        handleNewDescription(description);
        // need to go directly to stdout to avoid Output parsing prior to Exec handling
        System.out.println(String.format("^no_line^\t^b^%s^r^ \t", description.getMethodName()));
    }

    @Override public void testFinished(Description description) throws Exception {
        // need to go directly to stdout to avoid Output parsing prior to Exec handling
        if (failures.containsKey(description)) {
            Failure failure = failures.get(description);
            System.out.println(String.format("^no_prefix^^red^^i^ \u2620 FAILURE \u2620 ^r^ [ %s ]", failure.getMessage()));
        } else {
            System.out.println("^no_prefix^^green^^i^ \u2713 SUCCESS \u2713 ^r^");
        }
    }

    @Override public void testFailure(Failure failure) throws Exception {
        failures.put(failure.getDescription(), failure);
    }

    @Override public void testIgnored(Description description) throws Exception {
        handleNewDescription(description);
        System.out.println(String.format("^no_line^\t^b^%s^r^ \t", description.getMethodName()));
        System.out.println("^no_prefix^^yellow^^i^ \u26A0 IGNORED \u26A0 ^r^");
    }

    private void handleNewDescription(Description description) {
        if (!methodNameOffsets.containsKey(description.getClassName())) {
            methodNameOffsets.put(description.getClassName(), 0); // TODO - method name pad
            Output.print("^b^%s^r^", description.getClassName());
        }
    }

}
