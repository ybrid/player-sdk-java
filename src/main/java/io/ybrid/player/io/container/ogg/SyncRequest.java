/*
 * Copyright (c) 2020 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.container.ogg;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

/**
 * The SyncRequest is a helper to represent requests to a backend I/O-layer for
 * synchronisation of input data and decoding.
 */
public final class SyncRequest {
    private final int skip;
    private final int read;
    private final @Nullable Integer valid;

    /**
     * Main constructor.
     * @param skip Amount of bytes to skip.
     * @param read Amount of additional bytes to read.
     * @param valid Amount of valid bytes found or {@code null} if none.
     */
    @Contract(pure = true)
    public SyncRequest(int skip, int read, @Nullable Integer valid) {
        this.skip = skip;
        this.read = read;
        this.valid = valid;
    }

    /**
     * Request a skip.
     * A skip is removing a number of bytes at the start of the buffer.
     * @return Skip returned number of bytes.
     */
    @Contract(pure = true)
    public int getSkip() {
        return skip;
    }

    /**
     * Request additional data to be read.
     * A read will add a number of bytes at the end of the input.
     * @return Read returned number of extra bytes.
     */
    @Contract(pure = true)
    public int getRead() {
        return read;
    }

    /**
     * Return a number of valid bytes found or {@code null}.
     * The given number of bytes can be removed from the buffer and processed
     * after the number of bytes returned by {@link #getSkip()} as been skipped from the buffer.
     *
     * @return The number of valid bytes or {@code null}.
     */
    @Contract(pure = true)
    public @Nullable Integer getValid() {
        return valid;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "SyncRequest{" +
                "skip=" + skip +
                ", read=" + read +
                ", valid=" + valid +
                "}";
    }
}
