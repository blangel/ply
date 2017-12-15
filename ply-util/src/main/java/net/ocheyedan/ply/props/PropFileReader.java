package net.ocheyedan.ply.props;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * User: blangel
 * Date: 2/17/12
 * Time: 4:26 PM
 *
 * Loads a {@link PropFile} from an {@link InputStreamReader}.
 */
public interface PropFileReader {
    
    @SuppressWarnings("serial")
    static final class Invalid extends RuntimeException {

        final String invalidEntry;

        final String fileName;

        Invalid(String invalidEntry, String fileName, String message) {
            super(message);
            this.invalidEntry = invalidEntry;
            this.fileName = fileName;
        }
    }

    /**
     * Reads lines from from a {@link java.io.BufferedReader} according to {@link java.io.BufferedReader#readLine()}.  
     * Each line is trimmed according to {@link String#trim()} and then each escaped '=' character is un-escaped.
     * Note, in this implementation only lines starting with '#' are considered to be comments (as opposed to
     * the {@link java.util.Properties} class which also considers '!' characters).
     * Additionally, only the '=' character is considered to separate key from value (as opposed to the 
     * {@link java.util.Properties} class which also considers ':' characters).
     * The file is encoded via the {@link BufferedReader} and not forced to be {@literal ISO-8859-1} as it is with
     * the {@link java.util.Properties} class.
     * Also, the only thing one needs to escape is the '#' character, spaces within a key do not need to be
     * escaped as the only key to value delimiter is the '#' character.  However, like {@link java.util.Properties},
     * whitespace is not allowed as the start of the key (it will be trimmed by this implementation).
     */
    static final PropFileReader Default = new PropFileReader() {
        
        class ParseResult {
            private final Boolean complete;
            private final String key;
            private final String value;
            private ParseResult(Boolean complete, String key, String value) {
                this.complete = complete;
                this.key = key;
                this.value = value;
            }
        }

        @Override public void load(BufferedReader reader, PropFile into) throws IOException {
            if ((reader == null) || (into == null)) {
                throw new NullPointerException("The BufferedReader and PropFile cannot be null.");
            }
            String line;
            StringBuilder commentsBuffer = new StringBuilder();
            ParseResult parsing = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() && (parsing == null)) {
                    // continue;
                } else if (line.startsWith("#")) {
                    if (commentsBuffer.length() > 0) {
                        commentsBuffer.append("\n");
                    }
                    commentsBuffer.append(line.substring(1));
                } else {
                    ParseResult parsedLine = parse(line);
                    if (parsing != null) {
                        if (!parsedLine.key.isEmpty()) {
                            throw new Invalid(parsedLine.key, into.context().name, "Properties may only have one key.");
                        }
                        parsing = new ParseResult(false, parsing.key, String.format("%s%s", parsing.value, parsedLine.value));
                    } else {
                        parsing = parsedLine;
                    }
                    if (parsedLine.complete) {
                        if (parsing.key.isEmpty()) {
                            throw new Invalid(line, into.context().name, "Keys must be non-empty.");
                        } else if (into.contains(parsing.key)) {
                            throw new Invalid(parsing.key, into.context().name, "Keys must be unique.");
                        }
                        into.add(parsing.key.trim(), parsing.value.trim(), commentsBuffer.toString());
                        parsing = null;
                        commentsBuffer = new StringBuilder();
                    }
                }
            }
        }

        @SuppressWarnings("fallthrough")
        private ParseResult parse(String line) {
            StringBuilder buffer = new StringBuilder();
            String key = "", value;
            boolean isEscaped = false;
            for (char character : line.toCharArray()) {
                switch (character) {
                    case '\\':
                        isEscaped = true;
                        break;
                    case '=':
                        if (isEscaped || !key.isEmpty()) {
                            buffer.append('=');
                        } else {
                            key = buffer.toString();
                            buffer = new StringBuilder();
                        }
                        isEscaped = false;
                        break;
                    case ':':
                    case ' ':
                        if (isEscaped && key.isEmpty()) {
                            isEscaped = false; // eliminate ':' and ' ' escapes within key to conform to {@link Properties}
                        }
                    default:
                        if (isEscaped) {
                            buffer.append('\\');
                        }
                        buffer.append(character);
                        isEscaped = false;
                }
            }
            if (isEscaped) {
                buffer.append('\\');
            }
            value = buffer.toString();
            if (value.endsWith("\\")) {
                return new ParseResult(false, key, value.substring(0, value.length() - 1));
            } else {
                return new ParseResult(true, key, value);
            }
        }
    };

    /**
     * Caller is responsible for calling {@link java.io.BufferedReader#close()}.
     * @param reader from which to load the properties into the {@link PropFile}, {@code into}.
     * @param into the {@link PropFile} into which to load properties from {@code reader}.
     * @throws Invalid if the properties file represented by {@code reader} is invalid (i.e., duplicate keys, or no key
     *                 on a non-comment, non-blank line).
     * @throws IOException @see {@link java.io.BufferedReader#read()}
     */
    void load(BufferedReader reader, PropFile into) throws Invalid, IOException;

}
