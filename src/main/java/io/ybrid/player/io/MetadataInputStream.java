/*
 * Copyright (c) 2019 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.ybrid.player.io;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This implements a {@link InputStream} that adds support of {@link Metadata}.
 */
public class MetadataInputStream extends InputStream {
    private final ByteDataSource source;
    private Metadata metadata;
    private PlayoutInfo playoutInfo;
    private byte[] buffer;
    private int offset;
    private int readLimit = 0;
    private boolean eof = false;

    /**
     * This creates an instance using a {@link ByteDataSource}.
     * @param source The backend to use.
     */
    public MetadataInputStream(ByteDataSource source) {
        this.source = source;
    }

    /**
     * This returns the {@link Metadata} for an the next byte that can be read.
     *
     * This may re-fill internal buffers.
     *
     * @return The {@link Metadata} for the next byte or null.
     * @throws IOException I/O-Errors as thrown by the backend.
     */
    public Metadata getMetadata() throws IOException {
        fillBuffer();
        return metadata;
    }

    /**
     * This returns the {@link PlayoutInfo} for an the next byte that can be read.
     *
     * This may re-fill internal buffers.
     *
     * @return The {@link PlayoutInfo} for the next byte or null.
     * @throws IOException I/O-Errors as thrown by the backend.
     */
    public PlayoutInfo getPlayoutInfo() throws IOException {
        fillBuffer();
        return playoutInfo;
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int ret = read(buf, 0, 1);
        if (ret != 1)
            return -1;
        return buf[0];
    }

    private void fillBuffer() throws IOException {
        ByteDataBlock block;

        if (buffer != null && buffer.length == offset) {
            buffer = null;
            offset = 0;
        }

        if (buffer != null)
            return;

        try {
            block = source.read();

            buffer = block.getData();
            metadata = block.getMetadata();
        } catch (EOFException ignored) {
            eof = true;
            buffer = null;
            metadata = null;
        }
    }

    private int pump(byte[] b, int off, int len) throws IOException {
        int todo;

        fillBuffer();

        if (buffer == null)
            return 0;

        todo = buffer.length - offset;
        if (todo > len)
            todo = len;

        System.arraycopy(buffer, offset, b, off, todo);
        offset += todo;

        fillBuffer();

        return todo;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = 0;
        int res;

        if (readLimit > 0 && len > readLimit)
            len = readLimit;

        while (len > 0) {
            res = pump(b, off, len);
            if (res <= 0)
                break;

            off += res;
            len -= res;
            ret += res;
        }

        if (ret == 0 && eof)
            throw new EOFException();

        return ret;
    }

    /**
     * Gets the maximum amount of bytes a call to {@link #read(byte[], int, int)} or
     * {@link #read(byte[])} will return.
     * @return The read limit in [byte].
     */
    public int getReadLimit() {
        return readLimit;
    }

    /**
     * Sets the maximum amount of bytes a call to {@link #read(byte[], int, int)} or
     * {@link #read(byte[])} can return. If {@code len} or {@code b.length} is bigger than
     * the read limit they are replaced by the read limit.
     *
     * @param readLimit The read limit in [byte] or 0 for no limit.
     * @throws IllegalArgumentException thrown if {@code readLimit} is negative.
     */
    public void setReadLimit(int readLimit) throws IllegalArgumentException {
        if (readLimit < 0)
            throw new IllegalArgumentException("readLimit must be > 0 but was given as " + readLimit);
        this.readLimit = readLimit;
    }

    @Override
    public void close() throws IOException {
        super.close();
        source.close();
    }
}
