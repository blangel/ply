package net.ocheyedan.ply.props;

import java.util.ArrayList;
import java.util.List;

/**
 * User: blangel
 * Date: 3/20/15
 * Time: 9:06 AM
 *
 * Assists scripts in leveraging prefixed properties to create N number of generic arguments.  Generic arguments are
 * those things passed to a script which the script doesn't explicitly define.  E.g. the 'exec' script can
 * take 0...N number of program arguments as well as system arguments.  It is inflexible to define these property names
 * statically. Instead the script can leverage the convention that any property starting with 'propertyArg' is a property
 * argument. The remaining property key name is the argument name and the value if present can be appended to the
 * argument.
 */
public class PrefixedProps {

    /**
     * Any property starting with {@code propertyPrefix} will be stripped of the prefix and added to the return list
     * If the value of the property is present it will be appended to the stripped property name.
     * E.g. Assuming {@code propertyPrefix} is "args" and the {@code context} is "compiler"
     * then the command "ply set argsfoo='=bar' in compiler" will result
     * in the following argument "foo=bar"
     *
     * @param context        the context
     * @param propertyPrefix the property prefix to match
     * @return the matched properties
     */
    public static List<String> getArguments(Context context, String propertyPrefix) {
        return getArguments(context, propertyPrefix, "");
    }

    /**
     * Any property starting with {@code propertyPrefix} will be stripped of the prefix and added to the return list
     * If the value of the property is present it will be appended to the stripped property name.
     * E.g. Assuming {@code propertyPrefix} is "args" and the {@code context} is "compiler"
     * then the command "ply set argsfoo='=bar' in compiler" will result
     * in the following argument "foo=bar"
     *
     * @param context        the context
     * @param propertyPrefix the property prefix to match
     * @param separator to separate the key value from the key name
     * @return the matched properties
     */
    public static List<String> getArguments(Context context, String propertyPrefix, String separator) {
        PropFileChain chain = Props.get(context);
        return getArguments(chain, propertyPrefix, separator);
    }

    /**
     * Any property starting with {@code propertyPrefix} will be stripped of the prefix and added to the return list
     * If the value of the property is present it will be appended to the stripped property name.
     * E.g. Assuming {@code propertyPrefix} is "args" and the {@code context} is "compiler"
     * then the command "ply set argsfoo='=bar' in compiler" will result
     * in the following argument "foo=bar"
     *
     * @param props to match
     * @param propertyPrefix the property prefix to match
     * @return the matched properties
     */
    public static List<String> getArguments(PropFileChain props, String propertyPrefix) {
        return getArguments(props, propertyPrefix, "");
    }

    /**
     * Any property starting with {@code propertyPrefix} will be stripped of the prefix and added to the return list
     * If the value of the property is present it will be appended to the stripped property name.
     * E.g. Assuming {@code propertyPrefix} is "args" and the {@code context} is "compiler" and the separator is ""
     * then the command "ply set argsfoo='=bar' in compiler" will result
     * in the following argument "foo=bar"
     * If the separator where '=' then the following would result in the same returned argument:
     * "ply set argsfoo=bar in compiler"
     *
     * @param props          to match
     * @param propertyPrefix the property prefix to match
     * @return the matched properties
     */
    public static List<String> getArguments(PropFileChain props, String propertyPrefix, String separator) {
        List<String> arguments = new ArrayList<String>(1);
        for (PropFile.Prop prop : props.props()) {
            if (prop.name.startsWith(propertyPrefix) && (prop.name.length() > propertyPrefix.length())) {
                String keyName = prop.name.substring(propertyPrefix.length());
                String keyValue = prop.value();
                arguments.add(String.format("%s%s", keyName, (isEmpty(keyValue) ? "" : String.format("%s%s", separator, keyValue))));
            }
        }
        return arguments;
    }

    private static boolean isEmpty(String value) {
        return ((value == null) || value.isEmpty());
    }

}
