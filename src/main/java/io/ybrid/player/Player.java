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

package io.ybrid.player;

import io.ybrid.api.SessionClient;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface is implemented by ybrid capable players.
 */
public interface Player extends MetadataProvider, SessionClient, Closeable {
    /**
     * Prepare the player for playback.
     *
     * This call may do I/O-operation and may block.
     *
     * @throws IOException Thrown when there is any problem with the I/O.
     */
    void prepare() throws IOException;

    /**
     * Starts playback.
     *
     * If not called before this heaves as if it would call {@link #prepare()} before being called.
     *
     * @throws IOException Thrown when there is any problem with the I/O.
     */
    void play() throws IOException;

    /**
     * Stops the playback.
     *
     * After stop the player instance must not be reused.
     *
     * @throws IOException Thrown when there is any problem with the I/O.
     */
    void stop() throws IOException;
}
