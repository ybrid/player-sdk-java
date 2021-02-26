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

package io.ybrid.player.io.codec.opus;

import org.jetbrains.annotations.Contract;

/**
 * Audio Frame sizes. See RFC 6716 Section 2.1.4.
 */
public enum FrameSize {
    /**
     * 2.5ms.
     */
    FRAME_SIZE_2_5(120),
    /**
     * 5ms.
     */
    FRAME_SIZE_5(240),
    /**
     * 10ms.
     */
    FRAME_SIZE_10(480),
    /**
     * 20ms. This is the default for most operations.
     */
    FRAME_SIZE_20(960),
    /**
     * 40ms.
     */
    FRAME_SIZE_40(1920),
    /**
     * 60ms.
     */
    FRAME_SIZE_60(2880);

    private final int audioFrameCount;

    FrameSize(int audioFrameCount) {
        this.audioFrameCount = audioFrameCount;
    }

    /**
     * Gets the amount of audio frames (that is the number of samples per channel) contained in one frame of the given size.
     * @return The amount of audio frames [1].
     */
    @Contract(pure = true)
    public int getAudioFrameCount() {
        return audioFrameCount;
    }
}
