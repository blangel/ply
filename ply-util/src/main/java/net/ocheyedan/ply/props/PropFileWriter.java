package net.ocheyedan.ply.props;

import net.ocheyedan.ply.Output;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * User: blangel
 * Date: 2/17/12
 * Time: 9:19 PM
 * 
 * Writes a {@link PropFile} to an {@link BufferedWriter}.
 */
public interface PropFileWriter {

    /**
     * Writes all {@link PropFile.Prop} objects from {@code propFile}'s {@link PropFile#props()} to {@code writer}.
     * Each {@link PropFile.Prop} objects' {@link PropFile.Prop#comments()} are written
     * before the property.  The property is composed of escaping usage of the '=' character within the
     * {@link PropFile.Prop#name} and {@link PropFile.Prop#unfilteredValue} values and then combining them with
     * the '=' character.
     */
    static final PropFileWriter Default = new PropFileWriter() {
        
        final Pattern commentRegex = Pattern.compile("([^\r\n&&[^\r]&&[^\n]]+)");
        
        @Override public void store(BufferedWriter writer, PropFile propFile) throws IOException {
            store(writer, propFile, false);
        }
        @Override public void store(BufferedWriter writer, PropFile propFile, boolean useFiltered) throws IOException {
            for (PropFile.Prop prop : propFile.props()) {
                if (!prop.comments().isEmpty()) {
                    Matcher matcher = commentRegex.matcher(prop.comments());
                    while (matcher.find()) {
                        String comment = matcher.group();
                        writer.write(String.format("#%s%n", comment));
                    }
                }
                String line = escape(prop, useFiltered);
                writer.write(String.format("%s%n", line));
            }
        }

        private String escape(PropFile.Prop prop, boolean useFiltered) {
            String key = escape(prop.name);
            String value = escape(useFiltered ? prop.value() : prop.unfilteredValue);
            return String.format("%s=%s", key, value);
        }
        @SuppressWarnings("fallthrough")
        private String escape(String string) {
            StringBuilder buffer = new StringBuilder();
            for (char character : string.toCharArray()) {
                switch (character) {
                    case '=':
                        buffer.append('\\');
                    default:
                        buffer.append(character);
                }
            }
            return buffer.toString();
        }
    };

    /**
     * Caller is responsible for calling {@link java.io.BufferedWriter#close()}.
     * @param writer into which to write the properties of {@code propFile}
     * @param propFile containing the values which to write to {@code writer}
     * @throws IOException @see {@link BufferedWriter#write(int)}
     */
    void store(BufferedWriter writer, PropFile propFile) throws IOException;

    /**
     * Caller is responsible for calling {@link java.io.BufferedWriter#close()}.
     *
     * @param writer   into which to write the properties of {@code propFile}
     * @param propFile containing the values which to write to {@code writer}
     * @param useFiltered if true will use the filtered value of the property value
     * @throws IOException @see {@link BufferedWriter#write(int)}
     */
    void store(BufferedWriter writer, PropFile propFile, boolean useFiltered) throws IOException;
    
}
