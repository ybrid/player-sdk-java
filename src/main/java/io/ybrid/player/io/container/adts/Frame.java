/*
 * Copyright (c) 2022 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.container.adts;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.audio.MultiChannelSignalInformation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

/**
 * A complete ADTS frame as {@link ByteDataBlock}.
 */
public class Frame extends ByteDataBlock implements MultiChannelSignalInformation {
    private final @NotNull Header header;

    private Frame(@NotNull Sync sync, PlayoutInfo playoutInfo, byte[] data, @NotNull Header header) {
        super(sync, playoutInfo, data);
        this.header = header;
    }

    /**
     * Parses a frame from a {@link ByteDataBlock} with a given {@link Header}.
     * @param block The block to use as input.
     * @param header The header for the given block.
     * @return The new frame object.
     * @see Header#Header(byte[])
     */
    @Contract("_, _ -> new")
    public static @NotNull Frame parse(@NotNull ByteDataBlock block, @NotNull Header header) {
        return new Frame(block.getSync(), block.getPlayoutInfo(), block.getData(), header);
    }

    /**
     * Gets the header for the current block.
     * @return The header.
     */
    public @NotNull Header getHeader() {
        return header;
    }

    @Override
    public @Range(from = 1, to = Integer.MAX_VALUE) int getNumberOfChannels() {
        return header.getChannelConfiguration().getCount();
    }

    @Override
    public @Range(from = 1, to = Integer.MAX_VALUE) int getSampleRate() {
        return header.getSamplingFrequency().getFrequency();
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getLengthAsFrames() {
        return 1024;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "data.length=" + data.length +
                ", header=" + header +
                '}';
    }
}
