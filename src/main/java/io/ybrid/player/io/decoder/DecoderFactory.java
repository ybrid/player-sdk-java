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

import io.ybrid.api.util.MediaType;
import io.ybrid.api.util.QualityMap.MediaTypeMap;
import io.ybrid.player.io.DataSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * This interface is implemented by factory classes that build {@link Decoder} instances.
 */
public interface DecoderFactory {
    /**
     * Build a new {@link Decoder} based on the dataSource provided.
     *
     * This must not call {@link DataSource#read()} on the provided {@link DataSource}.
     *
     * @param dataSource The {@link DataSource} used to read data from.
     * @return The {@link Decoder} that has been build.
     */
    Decoder getDecoder(@NotNull DataSource dataSource);

    /**
     * Query formats supported by the decoder and their corresponding weights.
     *
     * @return The list of supported formats.
     * @deprecated This method should no longer be called.
     *             {@link #getSupportedMediaTypes()} should be used for calls.
     *             Implementations must provide {@link #getSupportedMediaTypes()}
     */
    @Deprecated
    @ApiStatus.ScheduledForRemoval
    default @NotNull Map<String, Double> getSupportedFormats() {
        return getSupportedMediaTypes().toStringDoubleMap();
    }

    /**
     * Query formats supported by the decoder and their corresponding weights.
     *
     * @return The list of supported formats.
     */
    default @NotNull MediaTypeMap getSupportedMediaTypes() {
        final @NotNull MediaTypeMap map = new MediaTypeMap();
        for (final @NotNull Map.Entry<String, Double> entry : getSupportedFormats().entrySet()) {
            map.put(new MediaType(entry.getKey()), entry.getValue());
        }
        return map;
    }
}
