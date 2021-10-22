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
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.sound.sampled.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

@ApiStatus.Internal
class Javax extends Base {
    private final static ByteOrder byteOrder = ByteOrder.nativeOrder();

    private @Nullable SourceDataLine line;

    @Override
    protected boolean available() {
        final @NotNull AudioFormat format = new AudioFormat(48000, 16, 2, true,
                byteOrder.equals(ByteOrder.BIG_ENDIAN));
        final @NotNull DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return AudioSystem.isLineSupported(info);
    }

    @Override
    protected synchronized void configureBackend(@NotNull PCMDataBlock block) throws IOException {
        try {
            final AudioFormat audioFormat = new AudioFormat(block.getSampleRate(),
                    16,
                    block.getNumberOfChannels(),
                    true,
                    byteOrder.equals(ByteOrder.BIG_ENDIAN));

            line = AudioSystem.getSourceDataLine(audioFormat);
            line.open(audioFormat);
        } catch (LineUnavailableException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void play() {
        super.play();
        Objects.requireNonNull(line).start();
    }

    @Override
    protected synchronized void deConfigureBackend() {
        if (line == null)
            return;

        line.drain();
        line.stop();
        line.close();
        line = null;
    }

    @Override
    protected void writeToBackend(@NotNull PCMDataBlock block) {
        final short[] data = block.getData();
        final @NotNull ByteBuffer buffer = ByteBuffer.allocate(data.length * 2).order(byteOrder);

        for (short val : data)
            buffer.putShort(val);

        buffer.position(0);

        Objects.requireNonNull(line).write(buffer.array(), 0, buffer.array().length);

        block.audible();
    }
}
