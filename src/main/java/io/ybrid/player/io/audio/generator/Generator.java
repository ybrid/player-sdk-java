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

package io.ybrid.player.io.audio.generator;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.api.metadata.source.Source;
import io.ybrid.api.metadata.source.SourceType;
import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.audio.PCMDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;

public class Generator implements PCMDataSource {
    private @NotNull Function function;
    private @NotNull Function.State state;
    private @NotNull Sync sync = Sync.Builder.buildEmpty(new Source(SourceType.SESSION));
    private @Nullable PlayoutInfo playoutInfo = null;
    private boolean valid = true;
    private int sampleRate = 48000;
    private int channels = 1;
    private int blockSize = 1024;
    private @Nullable Integer blocksLeft;

    public Generator() {
        setFunction(Function.createSilence());
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        if (blocksLeft != null) {
            if (blocksLeft == 0) {
                throw new EOFException();
            } else if (valid) {
                blocksLeft--;
            }
        }

        if (valid) {
            final short[] data = function.generate(state, sampleRate, channels, blockSize);
            return new PCMDataBlock(sync, playoutInfo, data, sampleRate, channels);
        } else {
            throw new EOFException();
        }
    }

    @Override
    public void close() {
        valid = false;
    }

    public @NotNull Sync getSync() {
        return sync;
    }

    public void setSync(@NotNull Sync sync) {
        this.sync = sync;
    }

    public @Nullable PlayoutInfo getPlayoutInfo() {
        return playoutInfo;
    }

    public void setPlayoutInfo(@Nullable PlayoutInfo playoutInfo) {
        this.playoutInfo = playoutInfo;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        if (sampleRate < 1)
            throw new IllegalArgumentException("Sample rate must be >=1Hz");

        this.sampleRate = sampleRate;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        if (channels < 1)
            throw new IllegalArgumentException("Number of channels must be >=1");

        this.channels = channels;
    }

    public int getBlockSize() {
        return blockSize;
    }

    public void setBlockSize(int blockSize) {
        this.blockSize = blockSize;
    }

    public @NotNull Function getFunction() {
        return function;
    }

    public @Nullable Integer getBlocksLeft() {
        return blocksLeft;
    }

    public void setBlocksLeft(@Nullable Integer blocksLeft) {
        this.blocksLeft = blocksLeft;
    }

    public void setFunction(@NotNull Function function) {
        this.function = function;
        this.state = function.createState();
    }
}
