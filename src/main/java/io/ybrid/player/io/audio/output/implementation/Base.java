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

package io.ybrid.player.io.audio.output.implementation;

import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.audio.output.AudioOutput;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@ApiStatus.Internal
abstract class Base implements AudioOutput {
    protected void openBackend() throws IOException {}
    protected void closeBackend() throws IOException {}
    protected abstract void configureBackend(@NotNull PCMDataBlock block) throws IOException;
    protected abstract void deConfigureBackend() throws IOException;
    protected abstract void writeToBackend(@NotNull PCMDataBlock block) throws IOException;

    @Contract(pure = true)
    protected abstract boolean available() throws Throwable;

    private boolean opened = false;
    private @Nullable PCMDataBlock config = null;

    @Override
    public synchronized void prepare(@NotNull PCMDataBlock block) throws IOException {
        if (!opened)
            openBackend();
        opened = true;

        if (config != null) {
            if (block.getSampleRate() != config.getSampleRate() ||
                    block.getNumberOfChannels() != config.getNumberOfChannels()) {
                deConfigureBackend();
                config = null;
            }
        }

        if (config == null)
            configureBackend(block);
        config = block;
    }

    @Override
    public void play() {}

    @Override
    public void write(@NotNull PCMDataBlock block) throws IOException {
        prepare(block);
        writeToBackend(block);
    }

    @Override
    public synchronized void close() throws IOException {
        if (config != null)
            deConfigureBackend();
        config = null;

        if (opened)
            closeBackend();
        opened = false;
    }
}
