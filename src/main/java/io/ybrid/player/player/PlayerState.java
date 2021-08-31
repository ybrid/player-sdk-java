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
     * The player has an error condition.
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
