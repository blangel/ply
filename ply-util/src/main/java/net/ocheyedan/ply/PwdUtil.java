package net.ocheyedan.ply;

import org.jasypt.contrib.org.apache.commons.codec_1_3.binary.Base64;
import org.jasypt.util.text.BasicTextEncryptor;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * User: blangel
 * Date: 1/9/13
 * Time: 1:06 PM
 *
 * Manages encryption/decryption of passwords.
 * Additionally this class defines the token {@link #PWD_REQUEST_TOKEN} which is used to allow child processes access
 * to {@link System#console()} for things like reading passwords.
 */
public final class PwdUtil {

    public static final class Request {
        private final AtomicBoolean pwd = new AtomicBoolean(false);

        private final AtomicReference<String> line = new AtomicReference<String>(null);

        public boolean isPwd() {
            return pwd.get();
        }
        public String getLine() {
            return line.get();
        }
    }

    public static final String PWD_REQUEST_TOKEN = "^password^";
    private static final int PWD_REQUEST_TOKEN_LENGTH = PWD_REQUEST_TOKEN.length();
    private static final Charset CHARSET = Charset.forName("UTF-8");

    private static final Request REQUEST = new Request();

    /**
     * Parses {@code line} and if it starts with {@link #PWD_REQUEST_TOKEN} then strips the prefix and returns
     * a {@link Request} object with the stripped line and value of true for {@link Request#pwd}, otherwise,
     * a value of false with the identical value of {@link Request#line} as was inputted.
     * @param line to check
     * @return a {@link Request} indicating whether {@code line} is a request to retrieve password information from
     *         the user.
     */
    public static Request isPwdRequest(String line) {
        if (line.startsWith(PWD_REQUEST_TOKEN)) {
            REQUEST.pwd.set(true);
            REQUEST.line.set(line.substring(PWD_REQUEST_TOKEN_LENGTH));
        } else {
            REQUEST.pwd.set(false);
            REQUEST.line.set(line);
        }
        return REQUEST;
    }

    /**
     * Encrypts {@code value} using the {@literal ply} pass-phrase and returns the encrypted value's {@literal Base64}
     * encoding.
     * TODO - use a master pass-phrase a la maven.  However, think about it more as anyone with access to the box can
     * TODO - decrypt maven's approach (see {@literal http://www.sonatype.com/people/2009/02/new-feature-maven-settings-password-encryption/}
     * TODO - and {@literal http://maven.apache.org/guides/mini/guide-encryption.html}).
     * @param value to encrypt
     * @return the encrypted value
     */
    public static String encrypt(String value) {
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword("P1y$");
        String encrypted = textEncryptor.encrypt(value);
        byte[] base64 = Base64.encodeBase64(encrypted.getBytes(CHARSET));
        return new String(base64, CHARSET);
    }

    /**
     * Decrypts {@code value} (assumed to be {@literal Base64} encoded) using the {@literal ply} pass-phrase.
     * TODO - see above regarding pass-phrase
     * @param value to decrypt
     * @return the decrypted value
     */
    public static String decrypt(String value) {
        byte[] decoded = Base64.decodeBase64(value.getBytes(CHARSET));
        BasicTextEncryptor textEncryptor = new BasicTextEncryptor();
        textEncryptor.setPassword("P1y$");
        return textEncryptor.decrypt(new String(decoded, CHARSET));
    }

    private PwdUtil() { }

}
