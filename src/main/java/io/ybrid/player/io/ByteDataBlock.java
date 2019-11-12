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

import io.ybrid.api.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements a {@link DataBlock} containing raw bytes.
 */
public class ByteDataBlock extends DataBlock {
    /**
     * Internal array to store bytes in.
     */
    protected byte[] data;

    /**
     * Create an instance using {@link Metadata} and an array of bytes.
     *
     * @param metadata The {@link Metadata} to use.
     * @param data The bytes to use.
     */
    public ByteDataBlock(Metadata metadata, byte[] data) {
        super(metadata);
        this.data = data;
    }

    /**
     * This creates a block by reading bytes off an {@link InputStream}.
     *
     * @param metadata The {@link Metadata} to use.
     * @param inputStream The {@link InputStream} to read the data from.
     * @param length The amount of bytes to read.
     * @throws IOException Thrown in case of I/O-Error.
     */
    public ByteDataBlock(Metadata metadata, InputStream inputStream, int length) throws IOException {
        super(metadata);
        data = new byte[length];
        int ret = inputStream.read(data);
        if (ret < 1)
            throw new EOFException();

        if (ret != data.length) {
            byte[] newBuffer = new byte[ret];
            System.arraycopy(data, 0, newBuffer, 0, ret);
            data = newBuffer;
        }
    }

    /**
     * Gets the stored data.
     *
     * @return Returns the bytes stored by this block.
     */
    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ByteDataBlock{" +
                "data.length=" + data.length +
                ", metadata=" + metadata +
                '}';
    }
}
