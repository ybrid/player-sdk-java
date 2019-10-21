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

package io.ybrid.player;

import io.ybrid.player.io.PCMDataBlock;

import java.io.Closeable;
import java.io.IOException;

/**
 * This interface is implemented by classes that allow the output of audio data.
 *
 * The general workflow is that after creation of an instance by means of a {@link AudioBackendFactory}
 * the {@link #prepare(PCMDataBlock)} is called. This opens the actual output device.
 * After {@link #prepare(PCMDataBlock)} {@link #play()} is called to set the backend into playback mode.
 * {@link #write(PCMDataBlock)} is called for each audio data block including the block passed to {@link #prepare(PCMDataBlock)}
 * if that block is to be played.
 * The interface user will call {@link #close()} when done.
 *
 */
public interface AudioBackend extends Closeable {
    /**
     * Prepares the backend for playback.
     *
     * @param block A block of PCM data used to obtain initial audio configuration. This block must not be played.
     * @throws IOException Thrown on backend related I/O-Error.
     */
    void prepare(PCMDataBlock block) throws IOException;

    /**
     * Start playback mode of the backend.
     */
    void play();

    /**
     * Set the backend into pause mode. Can be resumed by calling {@link #play()}.
     */
    void pause();

    /**
     * Writes an actual block of PCM data to the output buffer of the interface.
     *
     * This call blocks on average for the time represented by the block,
     *
     * @param block The block to write to the backend.
     * @throws IOException Thrown on backend related I/O-Error.
     */
    void write(PCMDataBlock block) throws IOException;
}
