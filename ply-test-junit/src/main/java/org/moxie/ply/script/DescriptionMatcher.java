package org.moxie.ply.script;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * User: blangel
 * Date: 10/13/11
 * Time: 6:06 PM
 *
 * Given an ant-style wildcard path, will match package.class.method's for test execution.
 */
public class DescriptionMatcher extends Filter {

    private static class MatchPart {
        private static enum Type { PKG_CLASS, METHOD }
        private static enum Location { STARTS, CONTAINS, ENDS }

        private final Type type;
        private final Location location;
        private final String matchSegment;

        private MatchPart(Type type, Location location, String matchSegment) {
            this.type = type;
            this.location = location;
            this.matchSegment = matchSegment;
        }

        private boolean matches(String match, Type type) {
            if (this.type != type) {
                return true;
            }
            switch (location) {
                case STARTS:
                    return match.startsWith(matchSegment);
                case CONTAINS:
                    return match.contains(matchSegment);
                case ENDS:
                    return match.endsWith(matchSegment);
                default:
                    throw new AssertionError("Programming error; unsupported MatchPart.Location " + location.name());
            }
        }
    }

    private final String match;

    public DescriptionMatcher(String match) {
        this.match = match;

    }

    @Override public boolean shouldRun(Description description) {
        return false;
    }

    @Override public String describe() {
        return match;
    }
}
