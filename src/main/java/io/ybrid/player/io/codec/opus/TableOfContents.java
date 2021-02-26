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
import org.jetbrains.annotations.NotNull;

/**
 * This represents the Table of Content as defined by RFC 6717 Section 3.1 extended by
 * the {@link FrameCount} as defined by Section 3.2.5.
 * The {@link FrameCount} is also included for non-{@link Code#ARBITRARY_NUMBER_OF_FRAMES} (Code 3) packets
 * with the values implied by the used {@link Code}.
 */
public class TableOfContents {
    private static final int FLAG_STEREO = 0x04;

    private final @NotNull Configuration configuration;
    private final boolean stereo;
    private final @NotNull Code code;
    private final @NotNull FrameCount frameCount;

    /**
     * Main constructor.
     * @param raw The raw packet as defined by RFC 6717.
     * @param offset The offset to the ToC byte as defined by Section 3.1.
     */
    public TableOfContents(@NotNull byte[] raw, int offset) {
        configuration = Configuration.valueOf((raw[offset] & 0xF8) >> 3);
        stereo = (raw[offset] & FLAG_STEREO) == FLAG_STEREO;
        code = Code.valueOf(raw[offset] & 0x03);

        switch (code) {
            case ONE_FRAME:
                frameCount = new FrameCount(true, 0, 1);
                break;
            case TWO_EQUAL_SIZED_FRAMES:
            case TWO_DIFFERENT_SIZED_FRAMES:
                frameCount = new FrameCount(true, 0, 2);
                break;
            case ARBITRARY_NUMBER_OF_FRAMES:
                frameCount = new FrameCount(raw, offset + 1);
                break;
            default:
                throw new IllegalArgumentException("Unknown code: " + code);
        }
    }

    /**
     * Returns the {@link Configuration} used by frames of this packet.
     * @return The used configuration.
     */
    @Contract(pure = true)
    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Returns whether frames in this packet encode stereo.
     * Note that this does not signal whether the decoder should decode the stream in
     * stereo or not.
     * @return Whether the frames are stereo.
     */
    @Contract(pure = true)
    public boolean isStereo() {
        return stereo;
    }

    /**
     * Returns the used {@link Code} used to encode multiple frames.
     * @return The used code.
     */
    @Contract(pure = true)
    public @NotNull Code getCode() {
        return code;
    }

    /**
     * Returns the {@link FrameCount} for this packet.
     * @return The frame count of this packet.
     */
    @Contract(pure = true)
    public @NotNull FrameCount getFrameCount() {
        return frameCount;
    }

    /**
     * Returns the number of audio frames contained in this packet.
     * This is sum of all audio frames of all Opus frames in this packet.
     * @return The number of contained audio frames (that is the number does not change for mono/stereo).
     */
    @Contract(pure = true)
    public int getAudioFrameCount() {
        return frameCount.getCount() * configuration.getFrameSize().getAudioFrameCount();
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "TableOfContents{" +
                "configuration=" + configuration +
                ", stereo=" + stereo +
                ", code=" + code +
                ", frameCount=" + frameCount +
                ", getAudioFrameCount()=" + (getAudioFrameCount()/48.) + "ms" +
                "}";
    }
}
