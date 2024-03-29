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

package io.ybrid.player.io.audio;

import org.jetbrains.annotations.Range;

import java.time.Duration;

public interface SignalInformation {
    /**
     * Gets the sample rate.
     *
     * @return Returns the sample rate in [Hz].
     */
    @Range(from = 1, to = Integer.MAX_VALUE) int getSampleRate();

    /**
     * This returns the length as units of frames.
     *
     * @return The length in [frame].
     */
    @Range(from = 0, to = Integer.MAX_VALUE) int getLengthAsFrames();

    /**
     * This returns the length.
     *
     * @return The length.
     */
    default Duration getLength() {
        /* This is the nicer implementation, yet it is incredibly slow on Android: */
        // return Duration.ofSeconds(getLengthAsFrames()).dividedBy(getSampleRate());

        /* This is the not so nice implementation, but way faster on Android: */
        long nanos = getLengthAsFrames();
        nanos *= 1_000_000_000L;
        nanos /= getSampleRate();
        return Duration.ofNanos(nanos);
    }

}
