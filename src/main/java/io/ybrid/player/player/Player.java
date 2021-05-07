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

package io.ybrid.player.player;

import io.ybrid.api.player.SimpleCommand;
import io.ybrid.api.transaction.Request;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.api.transaction.TransactionExecutionException;
import io.ybrid.player.io.audio.BufferStatusProvider;
import io.ybrid.player.transaction.RequestExecutor;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface is implemented by Ybrid capable players.
 */
public interface Player extends MetadataProvider, BufferStatusProvider, RequestExecutor, Closeable {
    /**
     * Prepare the player for playback.
     * <p>
     * This call may do I/O-operation and may block.
     * <p>
     * The default implementation makes use of {@link #executeTransaction(Request)}.
     *
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction prepare() throws TransactionExecutionException {
        return executeTransaction(SimpleCommand.PREPARE.makeRequest());
    }

    /**
     * Starts playback.
     * <p>
     * If not called before this behaves as if it would call {@link #prepare()} before being called.
     * <p>
     * The default implementation makes use of {@link #executeTransaction(Request)}.
     *
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction play() throws TransactionExecutionException {
        return executeTransaction(SimpleCommand.PLAY.makeRequest());
    }

    /**
     * Stops the playback.
     * <p>
     * After stop the player instance must not be reused.
     * <p>
     * The default implementation makes use of {@link #executeTransaction(Request)}.
     *
     * @throws TransactionExecutionException Thrown if a transaction failed while this method was still executing.
     */
    default @NotNull Transaction stop() throws TransactionExecutionException {
        return executeTransaction(SimpleCommand.STOP.makeRequest());
    }

    @Override
    default void close() throws IOException {
        stop();
    }
}
