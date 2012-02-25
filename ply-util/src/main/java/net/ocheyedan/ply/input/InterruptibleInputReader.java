package net.ocheyedan.ply.input;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

/**
 * User: blangel
 * Date: 2/10/12
 * Time: 9:20 AM
 * 
 * An implementation of {@link InputStreamReader} which respects {@link InterruptedException} events
 * by polling the status of the underlying {@link InputStream} (via {@link java.io.InputStream#available()})
 * and sleeping for {@link #pauseMs} duration in between polling calls.  The call to {@link Thread#sleep(long)} allows
 * for {@link InterruptedException} to be acted upon.
 * 
 * Note, if one does not need notification of interruption events then one shouldn't use this class and instead
 * prefer the non-interrupted blocking calls of the {@link InputStreamReader} directly as there is a performance
 * overhead to polling.
 * Additionally, if one can {@link java.io.Closeable#close()} the underlying {@link InputStream} one should do that
 * instead (as documented in {@literal Goetz}'s {@literal Java Concurrency in Practice} S 7.1.6), however,
 * certain streams should not be closed (for instance {@link System#in}) and so in these cases one may chose to
 * incur the performance overhead and use this class.
 */
public final class InterruptibleInputReader extends InputStreamReader {

    /**
     * Runtime wrapper around an {@link InterruptedException}.
     */
    @SuppressWarnings("serial")
    public final class InterruptedRuntimeException extends RuntimeException {
        public InterruptedRuntimeException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * The default amount of time to pause between poll attempts on the underlying {@link InputStream}
     */
    private static final long DEFAULT_PAUSE_MS = 100L;

    /**
     * The amount of time to pause between poll attempts on the underlying {@link InputStream}.
     */
    private final long pauseMs;

    public InterruptibleInputReader(InputStream in) {
        this(in, DEFAULT_PAUSE_MS);
    }

    public InterruptibleInputReader(InputStream in, long pauseMs) {
        super(in);
        this.pauseMs = pauseMs;
    }

    public InterruptibleInputReader(InputStream in, String charsetName, long pauseMs) throws UnsupportedEncodingException {
        super(in, charsetName);
        this.pauseMs = pauseMs;
    }

    public InterruptibleInputReader(InputStream in, Charset cs, long pauseMs) {
        super(in, cs);
        this.pauseMs = pauseMs;
    }

    public InterruptibleInputReader(InputStream in, CharsetDecoder dec, long pauseMs) {
        super(in, dec);
        this.pauseMs = pauseMs;
    }

    /**
     * Will poll by calling {@link #ready()} and if no data is ready then will {@link Thread#sleep(long)} for
     * {@link #pauseMs}.
     * If the {@link Thread#currentThread()} is interrupted while waiting for data to be read, this method will
     * throw a {@link InterruptedRuntimeException} with the cause being the actual {@link InterruptedException}
     * which caused the interruption or null if the interruption happened between calls to {@link Thread#sleep(long)}
     * and the polled check of {@link Thread#isInterrupted()}.
     * @param cbuf @see {@link InputStreamReader#read(char[], int, int)}
     * @param off @see {@link InputStreamReader#read(char[], int, int)}
     * @param len @see {@link InputStreamReader#read(char[], int, int)}
     * @return @see {@link InputStreamReader#read(char[], int, int)}
     * @throws IOException @see {@link InputStreamReader#read(char[], int, int)}
     * @throws InterruptedRuntimeException if the current thread was interrupted while waiting for data on the underlying
     *                                     {@link InputStream}
     */
    @Override public int read(char[] cbuf, int off, int len) throws IOException, InterruptedRuntimeException {
        while (!Thread.currentThread().isInterrupted()) {
            if (ready()) {
                return super.read(cbuf, off, len);
            } else {
                try {
                    Thread.sleep(pauseMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new InterruptedRuntimeException(ie);
                }
            }
        }
        throw new InterruptedRuntimeException(null);
    }

    /**
     * @return @see {@link InputStreamReader#read()}
     * @throws IOException @see {@link InputStreamReader#read()}
     * @throws InterruptedRuntimeException @see {@link #read(char[], int, int)}
     */
    @Override public int read() throws IOException, InterruptedRuntimeException {
        return super.read();
    }

    /**
     * @param target @see {@link InputStreamReader#read(CharBuffer)}
     * @return @see {@link InputStreamReader#read(CharBuffer)}
     * @throws IOException @see {@link InputStreamReader#read(CharBuffer)}
     * @throws InterruptedRuntimeException @see {@link #read(char[], int, int)}
     */
    @Override public int read(CharBuffer target) throws IOException, InterruptedRuntimeException {
        return super.read(target);
    }

    /**
     * @param cbuf @see {@link InputStreamReader#read(char[])}
     * @return @see {@link InputStreamReader#read(char[])}
     * @throws IOException @see {@link InputStreamReader#read(char[])}
     * @throws InterruptedRuntimeException @see {@link #read(char[], int, int)}
     */
    @Override public int read(char[] cbuf) throws IOException, InterruptedRuntimeException {
        return super.read(cbuf);
    }

    /**
     * Convenience method to read a line of text from the underlying {@link InputStream}.  A line of text
     * is all characters before, exclusive of, either '\n' or '\r'.
     * Note, the returned value may be the empty string if the first character read from the stream is one of the
     * line separator characters.  If {@link #read(char[], int, int)} returns -1 then null is returned.
     * @return the read line of text excluding the ending line separator(s) or null on EOF.
     * @throws IOException @see {@link InputStreamReader#read(char[], int, int)}
     * @throws InterruptedRuntimeException @see {@link #read(char[], int, int)}
     */
    public String readLine() throws IOException, InterruptedRuntimeException {
        StringBuilder buffer = new StringBuilder();
        char[] charBuffer = new char[1];

        for (;;) {
            int read = read(charBuffer, 0, charBuffer.length);
            if (read == -1) {
                return null;
            } else if (read > 0) {
                if ((charBuffer[0] == '\n') || (charBuffer[0] == '\r')) {
                    return buffer.toString();
                } else {
                    buffer.append(charBuffer[0]);
                }
            }
        }
    }

}
