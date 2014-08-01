package net.ocheyedan.ply.mvn;

import net.ocheyedan.ply.Output;

import java.util.Comparator;
import java.util.Map;

/**
 * User: blangel
 * Date: 11/10/11
 * Time: 7:11 PM
 *
 * Maven's versions can be specified with ranges, this class helps in parsing and resolving these ranges.
 * @see {@literal http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution#DependencyMediationandConflictResolution-DependencyVersionRanges}
 * @see {@literal http://www.sonatype.com/books/mvnref-book/reference/pom-relationships-sect-project-dependencies.html}
 *
 * Note, this is incomplete as maven allows for sets of disparate ranges (i.e., x < 1.0 or x > 2.0) and exclusions (x != 1.0)
 * but this class doesn't.  It doesn't because these two cases are not documented on the more official documentation
 * (found here http://www.sonatype.com/books/mvnref-book/reference/pom-relationships-sect-project-dependencies.html)
 * only on the developer doc (found here http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution#DependencyMediationandConflictResolution-DependencyVersionRanges)
 * Additionally, the ways of specifying each seem to conflict and make for a context-specific parsing; i.e., look at the
 * examples on the developer doc:
 * -1- (,1.0],[1.2,)	 x <= 1.0 or x >= 1.2. Multiple sets are comma-separated
 * -2- (,1.1),(1.1,)	 This excludes 1.1 if it is known not to work in combination with this library
 * These two examples are parsed differently.  The first joins the sets with OR the second with AND.  This is a confusing
 * notion and perhaps deprecated as the more official documentation doesn't mention them (but it does work with
 * at least maven version 3.0.3).
 * Attempts to parse the above cases will result in an exception which should halt execution and force the user to
 * intervene.
 */
public final class Version {

    /**
     * Indicates an incompatible and unfulfillable version range; i.e., [1.0,1.0)
     */
    @SuppressWarnings("serial")
    public static class Invalid extends RuntimeException { }

    /**
     * Indicates an attempt to parse a version range set (which is not supported); i.e., (,1.0],[1.2,)
     */
    @SuppressWarnings("serial")
    public static class UnsupportedRangeSet extends RuntimeException { }

    /**
     * Orders version strings according to {@literal http://docs.codehaus.org/display/MAVEN/Dependency+Mediation+and+Conflict+Resolution#DependencyMediationandConflictResolution-DependencyVersionRanges}
     */
    public static final Comparator<String> MAVEN_VERSION_COMPARATOR = new Comparator<String>() {
        @Override public int compare(String o1, String o2) {
            String[] parts1 = getParts(o1);
            String[] parts2 = getParts(o2);
            int major = compareNumber(parts1[0], parts2[0]);
            if (major != 0) {
                return major;
            }
            int minor = compareNumber(parts1[1], parts2[1]);
            if (minor != 0) {
                return minor;
            }
            int revision = compareNumber(parts1[2], parts2[2]);
            if (revision != 0) {
                return revision;
            }
            String qualifier1 = parts1[3];
            String qualifier2 = parts2[3];
            if (qualifier1.isEmpty() && !qualifier2.isEmpty()) {
                return 1;
            } else if (!qualifier1.isEmpty() && qualifier2.isEmpty()) {
                return -1;
            }
            int qualifier = qualifier1.compareTo(qualifier2);
            if (qualifier != 0) {
                return qualifier;
            }
            return compareNumber(parts1[4], parts2[4]);
        }
        private String[] getParts(String val) {
            String[] parts = new String[5];
            StringBuilder buffer = new StringBuilder();
            int index = -1, partsIndex = 0;
            while (++index < val.length()) {
                char character = val.charAt(index);
                if ((character == '.') || (character == '-')) {
                    parts[partsIndex++] = buffer.toString();
                    buffer = new StringBuilder();
                    if (character == '-') {
                        while (partsIndex < 3) {
                            parts[partsIndex++] = "0";
                        }
                    }
                    if (partsIndex == 4) {
                        parts[partsIndex++] = (index + 1 < val.length() ? val.substring(index + 1) : "");
                        break;
                    }
                } else {
                    buffer.append(character);
                }
            }
            if (buffer.length() > 0) {
                parts[partsIndex++] = buffer.toString();
            }
            while (partsIndex < 5) {
                String part = (partsIndex == 3 ? "" : "0");
                parts[partsIndex++] = part;
            }
            return parts;
        }
        private int compareNumber(String n1, String n2) {
            Integer num1;
            try {
                num1 = Integer.parseInt(n1);
            } catch (NumberFormatException nfe) {
                num1 = 0;
            }
            Integer num2;
            try {
                num2 = Integer.parseInt(n2);
            } catch (NumberFormatException nfe) {
                num2 = 0;
            }
            return num1.compareTo(num2);
        }
    };

    /**
     * Resolves {@code version}.
     * @see {@link Version} class documentation for details
     * @param version to resolve
     * @param baseResource to download the 'maven-metadata.xml' (or 'metadata.xml') file if need be
     * @param headers to use for making URL requests
     * @return the resolved version
     * @throws Invalid if the range is invalid
     * @throws UnsupportedRangeSet if the range includes a set which this method doesn't support
     */
    public static String resolve(String version, String baseResource, Map<String, String> headers) throws Invalid, UnsupportedRangeSet {
        if ((version == null) || (!version.startsWith("[") && !version.startsWith("("))) {
            return version;
        }
        boolean inclusiveStart = version.startsWith("["), inclusiveEnd = version.endsWith("]");
        String lower, upper;
        int index = 0;
        while (version.charAt(++index) != ',') { }
        lower = version.substring(1, index);
        char cur; int lowerEnd = index + 1;
        while ((cur = version.charAt(++index)) != ')' && (cur != ']')) { }
        upper = version.substring(lowerEnd, index);
        if (version.length() != (index + 1)) {
            throw new UnsupportedRangeSet();
        }
        lower = lower.trim();
        upper = upper.trim();
        if (lower.isEmpty() && upper.isEmpty()) {
            throw new Invalid();
        }
        // if inclusive and one provided, return that (same as simply using the 'soft' in that it is not technically correct
        // as if that version doesn't actually exist another should be automatically selected but failing in these cases
        // is not ridiculous [ when soft version not found ] as it is the intuitive reaction most would expect.
        if (inclusiveEnd && !upper.isEmpty()) {
            return upper;
        }
        // need to go looking for available versions...
        MavenMetadataParser parser = new MavenMetadataParser();
        MavenMetadataParser.Metadata metadata = parser.parseMetadata(baseResource, headers);
        if ((metadata == null) || ((metadata.latest == null) && (metadata.versions == null))) {
            Output.print("^warn^ Could not resolve the 'maven-metadata.xml' file from the repository at %s", baseResource);
            return null;
        }

        // start with the latest
        String latest = (metadata.latest == null ? metadata.versions.get(metadata.versions.size() - 1) : metadata.latest);
        // ensure the lower bound is satisfied with this latest
        if (!lower.isEmpty() && !withinLowerBound(inclusiveStart, lower, latest)) {
            Output.print("^warn^ Version ^b^%s^r^ falls outside of lower bound restriction: ^b^%s%s^r^", latest,
                    (inclusiveStart ? "[" :"("), lower);
            return null;
        }
        // if unbounded upper, return as lower bound for latest is already checked
        if (upper.isEmpty()) {
            return latest;
        }
        // check for exclusion on upper
        else {
            int compare = MAVEN_VERSION_COMPARATOR.compare(upper, latest);
            if (compare <= 0) {
                // try other versions in decreasing order (must also check that lower bound is still satisfied)
                if (metadata.versions != null) {
                    for (int i = metadata.versions.size() - 1; i > -1; i--) {
                        latest = metadata.versions.get(i);
                        if (MAVEN_VERSION_COMPARATOR.compare(upper, latest) > 0) {
                            if (!lower.isEmpty() && !withinLowerBound(inclusiveStart, lower, latest)) {
                                Output.print("^warn^ All available versions fall outside of lower bound restriction: ^b^%s%s^r^",
                                    (inclusiveStart ? "[" :"("), lower);
                                return null; // lower bound violated
                            }
                            return latest;
                        }
                    }
                }
                Output.print("^warn^ All available versions fall outside of upper bound restriction: ^b^%s%s^r^",
                        upper, (inclusiveEnd ? "[" : "("));
                return null;
            }
            return latest;
        }

    }

    private static boolean withinLowerBound(boolean inclusiveStart, String lower, String latest) {
        int compare = MAVEN_VERSION_COMPARATOR.compare(lower, latest);
        return !((compare > 0) || (!inclusiveStart && (compare == 0)));
    }

    private Version() { }

}
