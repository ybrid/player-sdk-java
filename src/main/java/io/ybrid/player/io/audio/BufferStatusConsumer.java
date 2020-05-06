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

package io.ybrid.player.io.audio;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This interface is implemented by classes accepting updates of state information
 * from the {@link Buffer}.
 */
public interface BufferStatusConsumer {
    /**
     * Called when the buffer state is updated.
     * @param status Current buffer state.
     */
    void onBufferStatusUpdate(@NotNull BufferStatus status);

    /**
     * Builds a adapter to log buffer state updates.
     * This is for debugging prurposes only.
     *
     * @param logger The logger to use.
     * @param level The loglevel to use.
     * @return The newly constructed consumer.
     */
    @Contract(pure = true, value = "_, _ -> new")
    @NotNull
    static BufferStatusConsumer buildLoggerAdapter(@NotNull Logger logger, @NotNull Level level) {
        return status -> logger.log(level, status.toString());
    }
}
