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

import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.MediaType;
import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.muxer.Demuxer;
import io.ybrid.player.io.muxer.Stream;
import io.ybrid.player.io.muxer.StreamUsage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DemuxerDecoder implements Decoder {
    private final @NotNull ByteDataSource source;
    private final @NotNull DecoderFactory decoderFactory;
    private @NotNull final Demuxer<?, ?> demuxer;
    private @Nullable Decoder decoder;
    private long accumulatedSkippedSamples = 0;
    private @Nullable Stream<?, ?, ?, ?> currentStream = null;

    private void assertDecoder() throws IOException {
        if (decoder != null)
            return;

        do {
            demuxer.iter();
            if (demuxer.isEofOnAutofill())
                throw new EOFException();
        } while (currentStream == null);

        decoder = decoderFactory.getDecoder(currentStream);
    }

    private void closeDecoder() throws IOException {
        if (decoder == null)
            return;

        accumulatedSkippedSamples += decoder.getSkippedSamples();
        decoder.close();
        decoder = null;
    }

    public static @NotNull Map<String, Double> getSupportedFormats() {
        @NonNls final Map<String, Double> list = new HashMap<>();

        list.put(MediaType.APPLICATION_OGG, 1.);
        list.put(MediaType.AUDIO_OGG, 1.);
        list.put(MediaType.ANY, 0.);

        return list;
    }

    public DemuxerDecoder(@NotNull ByteDataSource source, @NotNull DecoderFactory decoderFactory) throws IOException {
        this.source = source;
        this.decoderFactory = decoderFactory;

        if (!source.isValid())
            throw new IllegalStateException("Source is not valid");

        switch (Objects.requireNonNull(source.getContentType())) {
            case MediaType.APPLICATION_OGG:
            case MediaType.AUDIO_OGG:
                demuxer = new io.ybrid.player.io.muxer.ogg.Demuxer();
                break;
            default:
                throw new IOException("Input format not supported: " + source.getContentType());
        }

        demuxer.setAutofillSource(source);

        demuxer.setIsWantedCallback(s -> currentStream == null && s.getPrimaryStreamUsage().equals(StreamUsage.AUDIO));
        demuxer.setOnBeginOfStreamCallback(s -> currentStream = s);
        demuxer.setOnEndOfStreamCallback(s -> currentStream = currentStream == s ? null : currentStream);
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
        return !demuxer.isEofOnAutofill();
    }

    @Override
    public void close() throws IOException {
        closeDecoder();
        source.close();
    }
}
