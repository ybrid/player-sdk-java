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

import org.jetbrains.annotations.NotNull;

/**
 * The frame count of single Opus packets as defined by RFC 6716 Section 3.2.5.
 * See also {@link Code#ARBITRARY_NUMBER_OF_FRAMES}.
 */
public class FrameCount {
    private static final int FLAG_VBR = 0x80;
    private static final int FLAG_PADDING = 0x40;

    private final boolean vbr;
    private final int padding;
    private final int count;

    /**
     * Main constructor.
     * @param vbr The VBR bit.
     * @param padding The padding in bytes excluding the padding length.
     * @param count The number of frames in the packet.
     */
    public FrameCount(boolean vbr, int padding, int count) {
        this.vbr = vbr;
        this.padding = padding;
        this.count = count;
    }

    /**
     * Constructor based on the binary encoding defined in RFC 6716 Section 3.2.5.
     * This may read up to 3 bytes starting at the given offset.
     *
     * @param raw The array holding the raw data from the packet.
     * @param offset The offset to the frame count byte.
     */
    public FrameCount(@NotNull byte[] raw, int offset) {
        vbr = (raw[offset] & FLAG_VBR) == FLAG_VBR;

        if ((raw[offset] & FLAG_PADDING) == FLAG_PADDING) {
            if (raw[offset + 1] == (byte)255) {
                padding = (raw[offset + 2] & 0xFF) + 254;
            } else {
                padding = raw[offset + 1] & 0xFF;
            }
        } else {
            padding = 0;
        }

        count = raw[offset] & 0x3F;
    }

    /**
     * Returns whether the VBR bit is set.
     * @return The VBR bit.
     */
    public boolean isVBR() {
        return vbr;
    }

    /**
     * Returns the size of the used padding.
     * @return The padding excluding the padding length bytes.
     */
    public int getPadding() {
        return padding;
    }

    /**
     * Returns the number of audio frames.
     * @return The number of audio frames.
     */
    public int getCount() {
        return count;
    }
}
