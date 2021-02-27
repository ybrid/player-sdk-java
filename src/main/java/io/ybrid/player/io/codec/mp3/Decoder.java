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

package io.ybrid.player.io.codec.mp3;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.MetadataInputStream;
import io.ybrid.player.io.audio.PCMDataBlock;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;
import java.util.logging.Logger;

public class Decoder implements io.ybrid.player.io.decoder.Decoder {
    private static final @NonNls Logger LOGGER = Logger.getLogger(Decoder.class.getName());
    private final @NotNull MetadataInputStream inputStream;
    private final Bitstream bitstream;
    private final javazoom.jl.decoder.Decoder decoder;

    public Decoder(@NotNull ByteDataSource dataSource) {
        this.inputStream = new MetadataInputStream(dataSource);
        this.bitstream = new Bitstream(this.inputStream);
        this.decoder = new javazoom.jl.decoder.Decoder();
        LOGGER.info("Decoder created");
    }

    @Override
    public boolean isValid() {
        return true;
    }

    private static int getChannelCount(@NotNull Header header) {
        return header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        final @NotNull Sync sync = Objects.requireNonNull(inputStream.getSync());
        final PlayoutInfo playoutInfo = inputStream.getPlayoutInfo();
        final @NotNull Header header;
        final @NotNull SampleBuffer sampleBuffer;
        final @NotNull short[] pcm;

        try {
            header = bitstream.readFrame();
            sampleBuffer = (SampleBuffer) decoder.decodeFrame(header, bitstream);
            pcm = sampleBuffer.getBuffer().clone();
            bitstream.closeFrame();
        } catch (Throwable e) {
            LOGGER.warning("Decoder threw error: " + e);
            throw new IOException(e);
        }

        return new PCMDataBlock(sync, playoutInfo, pcm, header.frequency(), getChannelCount(header));
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("Closing bitstream");
        try {
            bitstream.close();
        } catch (BitstreamException e) {
            LOGGER.warning("Closing bitstream failed: " + e);
            throw new IOException(e);
        }
    }
}
