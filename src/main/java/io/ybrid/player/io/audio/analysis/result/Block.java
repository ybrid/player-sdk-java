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

package io.ybrid.player.io.audio.analysis.result;

import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.audio.MultiChannelSignalInformation;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public class Block implements MultiChannelSignalInformation {
    private final @NotNull PCMDataBlock block;
    private final Channel[] channels;

    public Block(@NotNull PCMDataBlock block) {
        this.block = block;
        this.channels = new Channel[block.getNumberOfChannels()];

        for (int channelIndex = 0; channelIndex < block.getNumberOfChannels(); channelIndex++) {
            this.channels[channelIndex] = new Channel(block.getSampleRate(), block.getNumberOfChannels(), channelIndex, block.getData());
        }
    }

    /**
     * Gets the analysis result for the individual channels.
     * @return The array of channels.
     */
    public Channel[] getChannels() {
        return channels;
    }

    /**
     * Gets the sample rate for this block.
     *
     * @return Returns the sample rate in [Hz].
     */
    @Override
    public int getSampleRate() {
        return block.getSampleRate();
    }

    /**
     * Gets the number of channel for this block.
     *
     * @return Returns the number of channels.
     */
    @Override
    public int getNumberOfChannels() {
        return block.getNumberOfChannels();
    }

    /**
     * This returns the length as units of frames.
     *
     * @return The length in [frame].
     */
    @Override
    public int getLengthAsFrames() {
        return block.getLengthAsFrames();
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "Block{" +
                "block=" + block +
                ", channels=" + Arrays.toString(channels) +
                "}";
    }
}
