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
