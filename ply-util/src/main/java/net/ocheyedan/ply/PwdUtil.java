package net.ocheyedan.ply;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.SecureRandom;
import javax.crypto.SecretKeyFactory;
import javax.crypto.SecretKey;
import java.util.Random;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.lang.RuntimeException;


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

    private static final Random RANDOM = new SecureRandom();
    private static final char[] PASSWORD = "P1y$".toCharArray();

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
        byte[] salt = new byte[8];
        RANDOM.nextBytes(salt);
        try {
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.ENCRYPT_MODE, key, new PBEParameterSpec(salt, 20));
            byte[] enc = pbeCipher.doFinal(value.getBytes(CHARSET));

            ByteArrayOutputStream saltPlusEnc = new ByteArrayOutputStream();
            saltPlusEnc.write(salt);
            saltPlusEnc.write(enc);

            BASE64Encoder encoder = new sun.misc.BASE64Encoder();
            return encoder.encode(saltPlusEnc.toByteArray());
        }
        catch (Exception e) {
            throw new RuntimeException("Encryption failed: " + e.getMessage());
        }
    }

    /**
     * Decrypts {@code value} (assumed to be {@literal Base64} encoded) using the {@literal ply} pass-phrase.
     * TODO - see above regarding pass-phrase
     * @param value to decrypt
     * @return the decrypted value
     */
    public static String decrypt(String value) {
        try {
            BASE64Decoder decoder = new sun.misc.BASE64Decoder();

            byte[] saltPlusEnc = decoder.decodeBuffer(value);
            byte[] salt = Arrays.copyOfRange(saltPlusEnc, 0, 8);
            byte[] enc = Arrays.copyOfRange(saltPlusEnc, 8, saltPlusEnc.length);

            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
            SecretKey key = keyFactory.generateSecret(new PBEKeySpec(PASSWORD));
            Cipher pbeCipher = Cipher.getInstance("PBEWithMD5AndDES");
            pbeCipher.init(Cipher.DECRYPT_MODE, key, new PBEParameterSpec(salt, 20));
            return new String(pbeCipher.doFinal(enc), "UTF-8");
        }
        catch (Exception e) {
            throw new RuntimeException("Decryption failed: " + e.getMessage());
        }
    }

    private PwdUtil() { }

}
