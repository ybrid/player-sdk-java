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

/**
 * The PlayerState enum allows communication of the current state of the player.
 * It is especially useful for updating the user interface.
 */
public enum PlayerState {
    /**
     * The player is currently in stopped state.
     * {@link Player#prepare()} must be called next.
     */
    STOPPED,
    /**
     * The player is playing.
     * {@link Player#stop()} can be called,
     */
    PLAYING,
    /**
     * The player is currently paused.
     */
    PAUSED,
    /**
     * The player is buffering.
     * The next state will likely (but not always) {@link #PLAYING}.
     */
    BUFFERING,
    /**
     * The player has a error condition.
     * {@link Player#close()} should be called on the player and the object discarded.
     */
    ERROR,
    /**
     * The player is preparing for playback.
     * This state happens after {@link Player#prepare()} was called and before
     * any other state is entered.
     */
    PREPARING;
}
