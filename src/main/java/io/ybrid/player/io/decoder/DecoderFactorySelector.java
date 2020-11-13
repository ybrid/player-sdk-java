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
import io.ybrid.player.io.DataSource;
import io.ybrid.player.io.MediaType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class DecoderFactorySelector implements DecoderFactory {
    private final @NotNull Set<@NotNull DecoderFactory> factories = new HashSet<>();

    public boolean add(@NotNull DecoderFactory decoderFactory) {
        return factories.add(decoderFactory);
    }

    public boolean remove(@NotNull DecoderFactory decoderFactory) {
        return factories.remove(decoderFactory);
    }

    @Override
    public Decoder getDecoder(@NotNull DataSource dataSource) {
        // if we have a ByteDataSource we can try to find the best one by the ContentType.
        if (dataSource instanceof ByteDataSource) {
            final @Nullable String contentType = ((ByteDataSource) dataSource).getContentType();
            if (contentType != null) {
                final @NotNull TreeMap<@NotNull Double, @NotNull DecoderFactory> map = new TreeMap<>(Comparator.reverseOrder());
                for (final @NotNull DecoderFactory factory : factories) {
                    final @Nullable Double quality = factory.getSupportedFormats().get(contentType);
                    if (quality != null)
                        map.put(quality, factory);
                }

                for (final @NotNull Map.Entry<@NotNull Double, @NotNull DecoderFactory> entry : map.entrySet()) {
                    final @Nullable Decoder decoder = entry.getValue().getDecoder(dataSource);
                    if (decoder != null)
                        return decoder;
                }
            }
        }

        // try all:
        for (final @NotNull DecoderFactory factory : factories) {
            final @Nullable Decoder decoder = factory.getDecoder(dataSource);
            if (decoder != null)
                return decoder;
        }

        // none found:
        return null;
    }

    @Override
    public @NotNull Map<String, Double> getSupportedFormats() {
        final @NotNull Map<@NotNull String, @NotNull Double> ret = new HashMap<>();

        for (final @NotNull DecoderFactory factory : factories) {
            for (final @NotNull Map.Entry<@NotNull String, @NotNull Double> entry : factory.getSupportedFormats().entrySet()) {
                final @NotNull String mediaType = entry.getKey();
                final double quality = entry.getValue();

                if (MediaType.isInternal(mediaType))
                    continue;

                if (ret.containsKey(mediaType) && ret.get(mediaType) >= quality)
                    continue;

                ret.put(mediaType, quality);
            }
        }

        return ret;
    }
}
