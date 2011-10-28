package org.moxie.ply.script;

import org.moxie.ply.Output;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * User: blangel
 * Date: 10/22/11
 * Time: 12:20 PM
 *
 * Filters a {@link File} in place by reading in the contents of the {@link File} line by line.  If the filtered
 * result is longer than the original line itself, the data is queued until there is enough room to write it.
 */
public class FileFilterer {

    /**
     * Provides a definition of a filter source, separating the filtering logic from the {@link FileFilterer} logic.
     */
    public static interface Provider {

        String filter(String value);

    }

    private static final int BUF_SIZE = 8192;

    private final File file;

    private final Provider provider;

    public FileFilterer(File file, Provider provider) {
        this.file = file;
        this.provider = provider;
    }

    public void filter() {
        FileChannel fc = null;
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
            fc = randomAccessFile.getChannel();
            /**
             * The javadoc for FileChannel#map indicates that memory mapping anything but large files (> tens of KB)
             * is generally less efficient than simply reading/writing from a file for most operating systems.  Because
             * of this, we are not memory mapping the file.
             * @see {@link FileChannel#map(java.nio.channels.FileChannel.MapMode, long, long)}
             */
            ByteBuffer readBuf = ByteBuffer.allocateDirect(BUF_SIZE);
            ByteBuffer writeBuf;
            long position = 0;
            int read = 0, totalRead = 0;

            do {

                int numberRead = 0;
                String converted;

                // read into the buffer
                do {
                    read = fc.read(readBuf);
                    numberRead += read;
                    totalRead += read;
                } while ((read != -1) && readBuf.hasRemaining() && (totalRead < fc.size()));

                // transform to string
                converted = readBuf.asCharBuffer().toString();

                // filter string
                converted = provider.filter(converted);

                // write filtered to writeBuf
                writeBuf = ByteBuffer.wrap(converted.getBytes(), 0, numberRead);

                // write numberRead bytes from writeBuf to the file
                fc.position(position);
                while (writeBuf.hasRemaining()) {
                    fc.write(writeBuf);
                }

                readBuf.rewind();
            } while (totalRead < fc.size());

        } catch (IOException ioe) {
            Output.print(ioe);
        } finally {
            if (fc != null) {
                try {
                    fc.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }
}
