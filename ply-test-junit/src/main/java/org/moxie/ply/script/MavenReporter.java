package org.moxie.ply.script;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;
import org.moxie.ply.Output;
import org.moxie.ply.props.Prop;
import org.moxie.ply.props.Props;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 2:41 PM
 *
 * Creates an XML report mimicking the format of the {@literal maven-surefire-plugin}.
 */
public class MavenReporter extends RunListener {

    private static class ReportTestSuite {

        private final String name;

        private final AtomicInteger failureCount = new AtomicInteger(0);

        private final AtomicInteger errorsCount = new AtomicInteger(0);

        private final AtomicInteger skippedCount = new AtomicInteger(0);

        private final AtomicInteger totalCount = new AtomicInteger(0);

        private final long startTime;

        private final AtomicLong duration = new AtomicLong();

        private final List<ReportTest> tests = new ArrayList<ReportTest>();

        private final Map<String, Integer> reportNameMap = new HashMap<String, Integer>();

        private ReportTestSuite(String name) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
        }

        private synchronized void startTest(Description description) {
            int index = tests.size();
            totalCount.addAndGet(1);
            ReportTest test = new ReportTest(description);
            tests.add(test);
            reportNameMap.put(test.name, index);
        }

        private synchronized void ignoreTest(Description description) {
            int index = tests.size();
            totalCount.addAndGet(1);
            skippedCount.addAndGet(1);
            ReportTest test = new ReportTest(description);
            test.finishByIgnore();
            tests.add(test);
            reportNameMap.put(test.name, index);
        }

        private synchronized void endTest(Description description) {
            int index = reportNameMap.get(description.getDisplayName());
            ReportTest test = tests.get(index);
            test.finish();
        }

        private synchronized void endTest(Failure failure) {
            int index = reportNameMap.get(failure.getDescription().getDisplayName());
            failureCount.addAndGet(1);
            ReportTest test = tests.get(index);
            test.finish(failure);
        }

        private synchronized void finish() {
            duration.set(System.currentTimeMillis() - startTime);
        }

        private String toXml() {
            StringBuilder xmlBuffer = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n<testsuite failures=\"");
            xmlBuffer.append(failureCount.get());
            xmlBuffer.append("\" time=\"");
            xmlBuffer.append(String.format("%.3f", (duration.get() / 1000.0f)));
            xmlBuffer.append("\" errors=\"");
            xmlBuffer.append(errorsCount.get());
            xmlBuffer.append("\" skipped=\"");
            xmlBuffer.append(skippedCount.get());
            xmlBuffer.append("\" tests=\"");
            xmlBuffer.append(totalCount.get());
            xmlBuffer.append("\" name=\"");
            xmlBuffer.append(name);
            xmlBuffer.append("\">\n  <properties>\n");
            for (String propertyName : System.getProperties().stringPropertyNames()) {
                xmlBuffer.append("    <property name=\"");
                xmlBuffer.append(StringEscapeUtils.escapeXml(propertyName));
                xmlBuffer.append("\" value=\"");
                xmlBuffer.append(StringEscapeUtils.escapeXml(System.getProperties().getProperty(propertyName)));
                xmlBuffer.append("\"/>\n");
            }
            xmlBuffer.append("  </properties>");
            for (ReportTest test : tests) {
                xmlBuffer.append("\n  ");
                xmlBuffer.append(test.toXml());
            }
            xmlBuffer.append("\n</testsuite>");
            return xmlBuffer.toString();
        }
    }

    private static class ReportTest {

        private final String name;

        private final String className;

        private final String methodName;

        private final long startTime;

        private final AtomicLong duration = new AtomicLong();

        private final AtomicReference<ReportTestFailure> failure = new AtomicReference<ReportTestFailure>(null);

        private final AtomicBoolean ignored = new AtomicBoolean(false);

        private ReportTest(Description description) {
            this.name = description.getDisplayName();
            this.className = description.getClassName();
            this.methodName = description.getMethodName();
            this.startTime = System.currentTimeMillis();
        }

        private void finish() {
            duration.set(System.currentTimeMillis() - startTime);
        }

        private void finishByIgnore() {
            finish();
            ignored.set(true);
        }

        private void finish(Failure failure) {
            finish();
            this.failure.set(new ReportTestFailure(failure));
        }

        private String toXml() {
            StringBuilder xmlBuffer = new StringBuilder("<testcase time=\"");
            xmlBuffer.append(String.format("%.3f", (duration.get() / 1000.0f)));
            xmlBuffer.append("\" classname=\"");
            xmlBuffer.append(className);
            xmlBuffer.append("\" name=\"");
            xmlBuffer.append(methodName);
            xmlBuffer.append("\"");
            if (failure.get() != null) {
                xmlBuffer.append(">\n    ");
                xmlBuffer.append(failure.get().toXml());
                xmlBuffer.append("\n  </testcase>");
            } else if (ignored.get()) {
                xmlBuffer.append(">\n    <skipped/>\n  </testcase>");
            } else {
                xmlBuffer.append("/>");
            }
            return xmlBuffer.toString();
        }
    }

    private static class ReportTestFailure {

        private final String message;

        private final String type;

        private final String trace;

        private ReportTestFailure(Failure failure) {
            this.message = failure.getMessage();
            this.trace = failure.getTrace();
            this.type = failure.getException().getClass().getName();
        }

        private String toXml() {
            StringBuilder xmlBuffer = new StringBuilder("<failure message=\"");
            xmlBuffer.append(StringEscapeUtils.escapeXml(message));
            xmlBuffer.append("\" type=\"");
            xmlBuffer.append(StringEscapeUtils.escapeXml(type));
            xmlBuffer.append("\">");
            xmlBuffer.append(StringEscapeUtils.escapeXml(trace));
            xmlBuffer.append("\n</failure>");
            return xmlBuffer.toString();
        }
    }

    private final Map<String, ReportTestSuite> testSuites = new HashMap<String, ReportTestSuite>();

    private ReportTestSuite current;

    @Override public void testRunFinished(Result result) throws Exception {
        synchronized (this) {
            if (current != null) {
                current.finish();
            }
        }
        // save the reports.
        Prop reportDirProp = Props.get("project", "test", "reports.dir");
        if (reportDirProp == null) {
            Output.print("^warn^ Could not find property project#test#reports.dir, skipping report save.");
            return;
        }
        File reportDir = new File(reportDirProp.value);
        reportDir.mkdirs();
        for (ReportTestSuite testSuite : testSuites.values()) {
            String fileName = "TEST-" + testSuite.name + ".xml";
            String xmlFileContent = testSuite.toXml();
            File file = new File(reportDir.getPath() + File.separator + fileName);
            FileWriter writer = null;
            try {
                file.createNewFile();
                writer = new FileWriter(file);
                writer.write(xmlFileContent);
            } catch (IOException ioe) {
                Output.print(ioe);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ioe) {
                        // ignore
                    }
                }
            }
        }
    }

    @Override public void testStarted(Description description) throws Exception {
        synchronized (this) {
            startTestSuiteIfNeeded(description);
            current.startTest(description);
        }
    }

    @Override public void testFinished(Description description) throws Exception {
        synchronized (this) {
            current.endTest(description);
        }
    }

    @Override public void testFailure(Failure failure) throws Exception {
        synchronized (this) {
            current.endTest(failure);
        }
    }

    @Override public void testIgnored(Description description) throws Exception {
        synchronized (this) {
            startTestSuiteIfNeeded(description);
            current.ignoreTest(description);
        }
    }

    private synchronized void startTestSuiteIfNeeded(Description description) {
        if (!testSuites.containsKey(description.getClassName())) {
            if (current != null) {
                current.finish();
            }
            current = new ReportTestSuite(description.getClassName());
            testSuites.put(current.name, current);
        }
    }
}
