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
import io.ybrid.api.metadata.Sync;
import org.jetbrains.annotations.NotNull;

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
     * Create an instance using an array of bytes.
     *
     * @param sync The {@link Sync} to use for the new DataBlock.
     * @param playoutInfo The {@link PlayoutInfo} to use for the new DataBlock.
     * @param data The bytes to use.
     */
    public ByteDataBlock(@NotNull Sync sync, PlayoutInfo playoutInfo, byte[] data) {
        super(sync, playoutInfo);
        this.data = data;
    }

    /**
     * This creates a block by reading bytes off an {@link InputStream}.
     *
     * @param sync The {@link Sync} to use for the new DataBlock.
     * @param playoutInfo The {@link PlayoutInfo} to use for the new DataBlock.
     * @param inputStream The {@link InputStream} to read the data from.
     * @param length The amount of bytes to read.
     * @throws IOException Thrown in case of I/O-Error.
     */
    public ByteDataBlock(@NotNull Sync sync, PlayoutInfo playoutInfo, InputStream inputStream, int length) throws IOException {
        super(sync, playoutInfo);
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
        //noinspection HardCodedStringLiteral
        return "ByteDataBlock{" +
                "data.length=" + data.length +
                ", sync=" + getSync() +
                "}";
    }
}
