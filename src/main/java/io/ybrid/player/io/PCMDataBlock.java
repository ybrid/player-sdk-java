/*
 * Copyright (c) 2019 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.audio.MultiChannelSignalInformation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This implements a {@link DataBlock} that holds PCM samples.
 */
public class PCMDataBlock extends DataBlock implements MultiChannelSignalInformation {
    /**
     * Internal storage for PCM data.
     */
    protected final short[] data;
    /**
     * Internal storage for sample rate of the block.
     */
    protected final int sampleRate;
    /**
     * Internal storage for the number of channels represented this block.
     */
    protected final int numberOfChannels;

    protected @Nullable Runnable onAudible = null;

    /**
     * Create a block from an array if samples.
     *
     * @param sync The {@link Sync} to use for the new DataBlock.
     * @param playoutInfo The {@link PlayoutInfo} to use for the new DataBlock.
     * @param data The samples to use as 16 bit PCM interleaved values.
     * @param sampleRate The sample rate of the signal in [Hz].
     * @param numberOfChannels The number of channels represented.
     */
    public PCMDataBlock(@NotNull Sync sync, PlayoutInfo playoutInfo, short[] data, int sampleRate, int numberOfChannels) {
        super(sync, playoutInfo);
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
    @Override
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the number of channel for this block.
     *
     * @return Returns the number of channels.
     */
    @Override
    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
     * This returns the length of the block as units of time.
     *
     * @return The length of this block in [s].
     * @deprecated Use {@link #getLength()} instead.
     */
    @Deprecated
    public double getBlockLength() {
        return (double)getData().length / (double)(getSampleRate() * getNumberOfChannels());
    }

    /**
     * This returns the length as units of frames.
     *
     * @return The length in [frame].
     */
    @Override
    public int getLengthAsFrames() {
        final int samples = getData().length;

        if ((samples % getNumberOfChannels()) != 0)
            throw new IllegalArgumentException("Number of samples is not a multiple of number of channels");

        return samples / getNumberOfChannels();
    }

    /**
     * Gets the callback for for when the block is audible.
     * For calling the callback {@link #audible()} should be used.
     *
     * @return The callback or {@code null}.
     * @see #audible()
     */
    @Contract(pure = true)
    public @Nullable Runnable getOnAudible() {
        return onAudible;
    }

    /**
     * Sets a callback for when the block is audible.
     * This callback can be called multiple times.
     *
     * @param onAudible The callback to set or {@code null}.
     */
    public void setOnAudible(@Nullable Runnable onAudible) {
        this.onAudible = onAudible;
    }

    /**
     * This should be called when the block is audible.
     * It calls the callback set by {@link #setOnAudible(Runnable)} if any.
     * This may also update statistics or provide hints to other parts of the runtime.
     */
    public void audible() {
        if (onAudible != null)
            onAudible.run();
    }
}
