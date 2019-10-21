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