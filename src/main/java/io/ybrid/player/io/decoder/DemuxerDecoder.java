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

package io.ybrid.player.io.decoder;

import io.ybrid.player.Decoder;
import io.ybrid.player.DecoderFactory;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.muxer.Demuxer;
import io.ybrid.player.io.muxer.Stream;
import io.ybrid.player.io.muxer.StreamUsage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.util.Objects;

public class DemuxerDecoder implements Decoder {
    private final @NotNull ByteDataSource source;
    private final @NotNull DecoderFactory decoderFactory;
    private @Nullable Demuxer<?, ?> demuxer;
    private @Nullable Decoder decoder;
    private long accumulatedSkippedSamples = 0;

    private void assertDemuxer() throws IOException {
        if (demuxer != null)
            return;

        if (!source.isValid())
            throw new IllegalStateException("Source is not valid");

        switch (Objects.requireNonNull(source.getContentType())) {
            // TODO: Add any supported MediaType. Also see below!
            default:
                throw new IOException("Input format not supported: " + source.getContentType());
        }

        // TODO: enable this as soon as we support any MediaType in the switch above.
        //demuxer.setAutofillSource(source);
    }

    private void assertDecoder() throws IOException {
        final @NotNull Stream<?, ?, ?, ?>[] stream = new Stream[1];

        if (decoder != null)
            return;

        assertDemuxer();
        assert demuxer != null;

        //noinspection ConstantConditions
        demuxer.setIsWantedCallback(s -> stream[0] == null && s.getPrimaryStreamUsage().equals(StreamUsage.AUDIO));
        demuxer.setOnBeginOfStreamCallback(s -> stream[0] = s);

        //noinspection ConstantConditions
        do {
            demuxer.iter();
            if (demuxer.isEofOnAutofill())
                throw new EOFException();
        } while (stream[0] == null);

        decoder = decoderFactory.getDecoder(stream[0]);
    }

    private void closeDecoder() throws IOException {
        if (decoder == null)
            return;

        accumulatedSkippedSamples += decoder.getSkippedSamples();
        decoder.close();
        decoder = null;
    }

    public DemuxerDecoder(@NotNull ByteDataSource source, @NotNull DecoderFactory decoderFactory) {
        this.source = source;
        this.decoderFactory = decoderFactory;
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        while (true) {
            assertDecoder();
            try {
                return Objects.requireNonNull(decoder).read();
            } catch (EOFException e) {
                closeDecoder();
            }
        }
    }

    @Override
    public long getSkippedSamples() {
        if (decoder == null) {
            return accumulatedSkippedSamples;
        } else {
            return accumulatedSkippedSamples + decoder.getSkippedSamples();
        }
    }

    @Override
    public boolean isValid() {
        if (demuxer == null) {
            return source.isValid();
        } else {
            return !demuxer.isEofOnAutofill();
        }
    }

    @Override
    public void close() throws IOException {
        demuxer = null;
        closeDecoder();
        source.close();
    }
}
