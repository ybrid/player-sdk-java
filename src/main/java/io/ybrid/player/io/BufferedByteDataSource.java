/*
 * Copyright (c) 2019 nacamar GmbH - YBRID®, a Hybrid Dynamic Live Audio Technology
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

/**
 * This implements a ByteDataSource that buffers blocks in order to implements {@link #mark()} and {@link #reset()}.
 *
 * This allows up to an implementation specific limits blocks to be buffered.
 *
 */
public class BufferedByteDataSource implements ByteDataSource {
    static private final int MAX_BUFFER = 4;

    private ByteDataSource backend;
    private LinkedList<ByteDataBlock> inputBuffer;
    private LinkedList<ByteDataBlock> outputBuffer;
    private boolean valid = true;

    /**
     * Creates an instance.
     * @param backend The backend object to use.
     */
    public BufferedByteDataSource(ByteDataSource backend) {
        this.backend = backend;
    }

    @Override
    public ByteDataBlock read() throws IOException {
        ByteDataBlock ret = null;

        if (outputBuffer != null) {
            try {
                ret = outputBuffer.remove();
            } catch (NoSuchElementException e) {
                outputBuffer = null;
            }
        }

        if (ret == null) {
            ret = backend.read();
        }

        if (inputBuffer != null) {
            if (inputBuffer.size() == MAX_BUFFER) {
                valid = false;
                inputBuffer = null;
            } else {
                inputBuffer.add(ret);
            }
        }

        return ret;
    }

    @Override
    public String getContentType() {
        return backend.getContentType();
    }

    @Override
    public void mark() {
        if (inputBuffer == null) {
            inputBuffer = new LinkedList<>();
        }

        inputBuffer.clear();
        valid = true;
    }

    @Override
    public void reset() {
        if (!valid)
            throw new IllegalStateException("DataSource is not valid");
        outputBuffer = inputBuffer;
        inputBuffer = null;
    }

    @Override
    public boolean isValid() {
        return valid && backend.isValid();
    }

    @Override
    public void close() throws IOException {
        valid = false;
        backend.close();
    }
}
