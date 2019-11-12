/*
 * Copyright (c) 2019 nacamar GmbH - YBRID®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io;

import io.ybrid.api.Metadata;

/**
 * This implements a {@link DataBlock} that holds PCM samples.
 */
public class PCMDataBlock extends DataBlock {
    /**
     * Internal storage for PCM data.
     */
    protected short[] data;
    /**
     * Internal storage for sample rate of the block.
     */
    protected int sampleRate;
    /**
     * Internal storage for the number of channels represented this block.
     */
    protected int numberOfChannels;

    /**
     * Create a block from an array if samples.
     *
     * @param metadata The {@link Metadata} to use.
     * @param data The samples to use as 16 bit PCM interleaved values.
     * @param sampleRate The sample rate of the signal in [Hz].
     * @param numberOfChannels The number of channels represented.
     */
    public PCMDataBlock(Metadata metadata, short[] data, int sampleRate, int numberOfChannels) {
        super(metadata);
        this.data = data;
        this.sampleRate = sampleRate;
        this.numberOfChannels = numberOfChannels;
    }

    /**
     * Gets the PCM data,
     *
     * @return The PCM data as stored by the block.
     */
    public short[] getData() {
        return data;
    }

    /**
     * Gets the sample rate for this block.
     *
     * @return Returns the sample rate in [Hz].
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the number of channel for this block.
     *
     * @return Returns the number of channels.
     */
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
     * This returns the length of the block as units of time.
     *
     * @return The length of this block in [s].
     */
    public double getBlockLength() {
        return (double)getData().length / (double)(getSampleRate() * getNumberOfChannels());
    }
}
