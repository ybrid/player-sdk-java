/*
 * Copyright 2019 nacamar GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
