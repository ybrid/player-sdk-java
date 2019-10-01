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

package io.ybrid.client.player.io;

import java.io.IOException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class BufferedDataSource implements DataSource {
    static private final int MAX_BUFFER = 4;

    private DataSource backend;
    private LinkedList<DataBlock> inputBuffer;
    private LinkedList<DataBlock> outputBuffer;
    private boolean valid = true;

    public BufferedDataSource(DataSource backend) {
        this.backend = backend;
    }

    @Override
    public DataBlock read() {
        DataBlock ret = null;

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
