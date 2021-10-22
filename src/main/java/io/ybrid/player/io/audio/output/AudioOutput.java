/*
 * Copyright (c) 2021 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.audio.output;

import io.ybrid.player.io.audio.PCMDataBlock;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface is implemented by classes that allow the output of audio data.
 *
 * The general workflow is that after creation of an instance by means of a {@link AudioOutputFactory}
 * the {@link #prepare(PCMDataBlock)} is called. This opens the actual output device.
 * After {@link #prepare(PCMDataBlock)} {@link #play()} is called to set the backend into playback mode.
 * {@link #write(PCMDataBlock)} is called for each audio data block including the block passed to {@link #prepare(PCMDataBlock)}
 * if that block is to be played.
 * The interface user will call {@link #close()} when done.
 *
 */
public interface AudioOutput extends Closeable {
    /**
     * Prepares the backend for playback.
     *
     * @param block A block of PCM data used to obtain initial audio configuration. This block must not be played.
     * @throws IOException Thrown on backend related I/O-Error.
     */
    void prepare(@NotNull PCMDataBlock block) throws IOException;

    /**
     * Start playback mode of the backend.
     */
    void play();

    /**
     * Writes an actual block of PCM data to the output buffer of the interface.
     *
     * This call blocks on average for the time represented by the block.
     * Also, this call is responsible to call {@link PCMDataBlock#audible()} on {@code block}
     * when the block is audible.
     *
     * @param block The block to write to the backend.
     * @throws IOException Thrown on backend related I/O-Error.
     */
    void write(@NotNull PCMDataBlock block) throws IOException;
}
