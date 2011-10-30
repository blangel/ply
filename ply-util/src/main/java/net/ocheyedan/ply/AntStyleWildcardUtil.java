package net.ocheyedan.ply;

import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 10/16/11
 * Time: 2:08 PM
 *
 * Provides methods to convert {@literal Ant} style wildcard expressions into {@literal regex} strings.
 */
public final class AntStyleWildcardUtil {

    /**
     * Converts {@code antStyleWildcard} into a {@literal regex} string.  Note, ant-style wildcards are assumed
     * to be package separated by the '/' character.
     * @param antStyleWildcard to convert
     * @return the converted {@literal regex} representation of {@code antStyleWildcard}
     */
    public static String regexString(String antStyleWildcard) {
        String regex = antStyleWildcard; // Pattern.quote?
        // replace all '?' with a single character match
        regex = regex.replaceAll("\\?", ".{1}");
        // replace all '**' with a match for any package
        regex = regex.replaceAll("\\*\\*", ".+?");
        // replace all remaining '*' with a match for any character not package separator
        return regex.replaceAll("\\*", "[^/]*?");
    }

    public static Pattern regex(String anyStyleWildcard) {
        return Pattern.compile(regexString(anyStyleWildcard));
    }

    private AntStyleWildcardUtil() { }

}
