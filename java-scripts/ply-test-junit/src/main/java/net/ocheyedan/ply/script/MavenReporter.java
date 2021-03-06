package net.ocheyedan.ply.script;

import net.ocheyedan.ply.props.Context;
import net.ocheyedan.ply.props.Props;
import net.ocheyedan.ply.script.print.PrivilegedOutput;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static net.ocheyedan.ply.props.PropFile.Prop;

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
            if (ReportTestFailure.isError(failure)) {
                errorsCount.addAndGet(1);
            } else {
                failureCount.addAndGet(1);
            }
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
                xmlBuffer.append(XmlEscaper.escapeXml(propertyName));
                xmlBuffer.append("\" value=\"");
                xmlBuffer.append(XmlEscaper.escapeXml(System.getProperties().getProperty(propertyName)));
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

        private final String exceptionType;

        private final String trace;

        private ReportTestFailure(Failure failure) {
            this.message = failure.getMessage();
            this.trace = failure.getTrace();
            this.exceptionType = failure.getException().getClass().getName();
        }

        private String toXml() {
            // be inline with surefire's 'error' notion (TODO - is this actually what surefire uses to distinguish an error?)
            String failureType = (isError(exceptionType) ? "error" : "failure");
            StringBuilder xmlBuffer = new StringBuilder("<");
            xmlBuffer.append(failureType);
            xmlBuffer.append(" message=\"");
            xmlBuffer.append(XmlEscaper.escapeXml(message));
            xmlBuffer.append("\" type=\"");
            xmlBuffer.append(XmlEscaper.escapeXml(exceptionType));
            xmlBuffer.append("\">");
            xmlBuffer.append(XmlEscaper.escapeXml(trace));
            xmlBuffer.append("\n    </");
            xmlBuffer.append(failureType);
            xmlBuffer.append(">");
            return xmlBuffer.toString();
        }

        private static boolean isError(Failure failure) {
            Throwable excep = failure.getException();
            return isError(excep.getClass().getName());
        }

        private static boolean isError(String exceptionType) {
            return exceptionType.startsWith("junit") ? false : true;
        }
    }

    /**
     * Code taken from commons-lang:commons-lang:2.6 Entities.XML; done to eliminate dependency when all that was
     * needed was < 20 lines of code.
     */
    private static class XmlEscaper {
        private static final Map<Character, String> XML_ESCAPES = new HashMap<Character, String>(5, 1.0f);
        static {
            XML_ESCAPES.put((char) 34, "quot"); // " - double-quote
            XML_ESCAPES.put((char) 38, "amp"); // & - ampersand
            XML_ESCAPES.put((char) 60, "lt"); // < - less-than
            XML_ESCAPES.put((char) 62, "gt"); // > - greater-than
            XML_ESCAPES.put((char) 39, "apos"); // XML apostrophe
        }
        private static String escapeXml(String str) {
            if (str == null) {
                return null;
            }
            // make the escaped buffer 10% larger to avoid resizing (which doubles the buffer)
            StringBuilder buffer = new StringBuilder((int) (str.length() + (str.length() * 0.1)));
            int len = str.length();
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                String entityName = XML_ESCAPES.get(c);
                if (entityName == null) {
                    if (c > 0x7F) {
                        buffer.append("&#");
                        buffer.append(Integer.toString(c, 10));
                        buffer.append(';');
                    } else {
                        buffer.append(c);
                    }
                } else {
                    buffer.append('&');
                    buffer.append(entityName);
                    buffer.append(';');
                }
            }
            return buffer.toString();
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
        Prop reportDirProp = Props.get("reports.dir", Context.named("project"));
        if (Prop.Empty.equals(reportDirProp)) {
            PrivilegedOutput.print("^warn^ Could not find property project.reports.dir, skipping report save.");
            return;
        }
        File reportDir = new File(reportDirProp.value());
        reportDir.mkdirs();
        for (ReportTestSuite testSuite : testSuites.values()) {
            String fileName = getReportName(testSuite.name);
            String xmlFileContent = testSuite.toXml();
            File file = new File(reportDir.getPath() + File.separator + fileName);
            FileWriter writer = null;
            try {
                file.createNewFile();
                writer = new FileWriter(file);
                writer.write(xmlFileContent);
            } catch (IOException ioe) {
                PrivilegedOutput.print(ioe);
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException ioe) {
                        throw new AssertionError(ioe);
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

    /**
     * @param test is the name of the test class
     * @return the maven report file name
     */
    public static String getReportName(String test) {
        return "TEST-" + test + ".xml";
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
