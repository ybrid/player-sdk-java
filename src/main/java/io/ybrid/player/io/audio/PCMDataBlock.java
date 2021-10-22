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

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.audio.analysis.result.Block;
import org.jetbrains.annotations.*;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This implements a {@link DataBlock} that holds PCM samples.
 */
public class PCMDataBlock extends DataBlock implements MultiChannelSignalInformation {
    static final @NonNls Logger LOGGER = Logger.getLogger(PCMDataBlock.class.getName());

    /**
     * Internal storage for PCM data.
     */
    protected final short[] data;
    /**
     * Internal storage for sample rate of the block.
     */
    protected final @Range(from = 1, to = Integer.MAX_VALUE) int sampleRate;
    /**
     * Internal storage for the number of channels represented this block.
     */
    protected final @Range(from = 1, to = Integer.MAX_VALUE) int numberOfChannels;

    protected final @NotNull Set<@NotNull Runnable> onAudible = new HashSet<>();

    /**
     * Create a block from an array if samples.
     *
     * @param sync The {@link Sync} to use for the new DataBlock.
     * @param playoutInfo The {@link PlayoutInfo} to use for the new DataBlock.
     * @param data The samples to use as 16 bit PCM interleaved values.
     * @param sampleRate The sample rate of the signal in [Hz].
     * @param numberOfChannels The number of channels represented.
     */
    public PCMDataBlock(@NotNull Sync sync,
                        PlayoutInfo playoutInfo,
                        short[] data,
                        @Range(from = 1, to = Integer.MAX_VALUE) int sampleRate,
                        @Range(from = 1, to = Integer.MAX_VALUE) int numberOfChannels) {
        super(sync, playoutInfo);
        this.data = data;
        this.sampleRate = sampleRate;
        this.numberOfChannels = numberOfChannels;

        if ((data.length % numberOfChannels) != 0) {
            LOGGER.log(Level.WARNING, "Creating questionable PCMDataBlock: Number of samples (" + data.length + ") is not a multiple of number of channels (" + numberOfChannels + ")", new IllegalArgumentException());
        }
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
    public @Range(from = 1, to = Integer.MAX_VALUE) int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the number of channel for this block.
     *
     * @return Returns the number of channels.
     */
    @Override
    public @Range(from = 1, to = Integer.MAX_VALUE) int getNumberOfChannels() {
        return numberOfChannels;
    }

    /**
     * This returns the length as units of frames.
     *
     * @return The length in [frame].
     */
    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getLengthAsFrames() {
        final int samples = getData().length;

        if ((samples % getNumberOfChannels()) != 0)
            throw new IllegalArgumentException("Number of samples (" + samples + ") is not a multiple of number of channels (" + getNumberOfChannels() + ")");

        return samples / getNumberOfChannels();
    }

    /**
     * Adds a callback for when the block is audible.
     * This callback can be called multiple times.
     *
     * @param runnable The callback to add.
     */
    public void onAudible(@NotNull Runnable runnable) {
        onAudible.add(runnable);
    }

    /**
     * This should be called when the block is audible.
     * It calls the callbacks scheduled by {@link #onAudible(Runnable)} if any.
     * This may also update statistics or provide hints to other parts of the runtime.
     */
    public void audible() {
        for (final @NotNull Runnable runnable : onAudible) {
            try {
                runnable.run();
            } catch (Throwable e) {
                LOGGER.warning("on audible handler " + runnable + " for PCM block " + this + " failed with " + e);
            }
        }
    }

    /**
     * Analyse the current block and return the result.
     *
     * @return The analyse result.
     */
    @ApiStatus.Experimental
    public @NotNull Block analyse() {
        return new Block(this);
    }

    /**
     * Creates a new block that is a sub block of this.
     * @param start The first frame to include in [frame].
     * @param end The last frame to include in [frame].
     * @return The resulting block.
     * @see #trim(int, int)
     */
    @Contract(pure = true)
    public @NotNull PCMDataBlock subBlock(int start, int end) {
        final int length = getLengthAsFrames();
        final short[] n;

        if (end < start || start < 0 || start > length || end > length)
            throw new IllegalArgumentException("start (" + start + ") or end (" + end + ") invalid. Total length is " + length);

        start *= numberOfChannels;
        end *= numberOfChannels;

        n = new short[end - start];
        System.arraycopy(data, start, n, 0, n.length);

        return new PCMDataBlock(getSync(), getPlayoutInfo(), n, sampleRate, numberOfChannels);
    }

    /**
     * Creates a new block that is a sub block of this.
     * @param startTrim How many frames to trim from the start in [frame].
     * @param endTrim How many frames to trim from the end in [frame].
     * @return The resulting block.
     * @see #subBlock(int, int)
     */
    @Contract(pure = true)
    public @NotNull PCMDataBlock trim(int startTrim, int endTrim) {
        return subBlock(startTrim, getLengthAsFrames() - endTrim);
    }
}
