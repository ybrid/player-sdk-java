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

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface is implemented by classes that allow reading data in a block-wise fashion.
 */
public interface DataSource extends Closeable {
    /**
     * Read a block of data.
     * @return The block read.
     * @throws IOException Any I/O-Error that happened during the read.
     */
    DataBlock read() throws IOException;

    /**
     * Mark the current position in the stream to return to later.
     * To return {@link #reset()} must be called.
     *
     * This may not be supported by all sources.
     */
    default void mark() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return to a position as marked by {@link #mark()}.
     *
     * See {@link #mark()} for details.
     */
    default void reset() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns whether the state of this source is still valid.
     *
     * @return Whether this source is still valid.
     */
    boolean isValid();
}
