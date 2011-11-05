package net.ocheyedan.ply.script;

import net.ocheyedan.ply.AntStyleWildcardUtil;
import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 6:06 PM
 *
 * Given an ant-style wildcard path, will match package.class#method's for test execution.
 */
public class DescriptionMatcher extends Filter {

    private final String match;

    private final Pattern classOnlyPattern;

    private final Pattern pattern;

    public DescriptionMatcher(String match) {
        this.match = match.replaceAll("\\.", "/"); // normalize to '/'
        String regex = this.match;
        String prefix = "", suffix = "";
        // if no package specified, use any
        if (!regex.contains("/") && !regex.startsWith("**")) {
            prefix = ".*?/";
        }
        // if no method specified, use any
        String packageClassOnly = regex;
        if (!regex.contains("#")) {
            suffix = "#.*";
        } else {
            packageClassOnly = regex.substring(0, regex.indexOf("#"));
        }

        regex = AntStyleWildcardUtil.regexString(regex);
        packageClassOnly = AntStyleWildcardUtil.regexString(packageClassOnly);
        this.pattern = Pattern.compile(prefix + regex + suffix);
        this.classOnlyPattern = Pattern.compile(prefix + packageClassOnly);
    }

    @Override public boolean shouldRun(Description description) {
        if (description.getMethodName() == null) {
            String toMatch = description.getClassName();
            toMatch = toMatch.replaceAll("\\.", "/"); // normalize to '/'
            if (!classOnlyPattern.matcher(toMatch).matches()) {
                return false;
            }
            for (Description child : description.getChildren()) {
                String fullMatch = toMatch + "#" + child.getMethodName();
                if (pattern.matcher(fullMatch).matches()) {
                    return true;
                }
            }
            return false;
        } else {
            String toMatch = (description.getClassName() + "#" + description.getMethodName());
            toMatch = toMatch.replaceAll("\\.", "/"); // normalize to '/'
            return pattern.matcher(toMatch).matches();
        }
    }

    @Override public String describe() {
        return match;
    }
}
