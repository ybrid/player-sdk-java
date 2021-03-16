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

import io.ybrid.api.util.MediaType;
import io.ybrid.api.util.QualityMap.MediaTypeMap;
import io.ybrid.api.util.QualityMap.Quality;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.DataSource;
import org.jetbrains.annotations.NotNull;

public class DecoderFactory implements io.ybrid.player.io.decoder.DecoderFactory {
    @Override
    public io.ybrid.player.io.decoder.Decoder getDecoder(@NotNull DataSource dataSource) {
        return new Decoder((ByteDataSource) dataSource);
    }

    @Override
    public @NotNull MediaTypeMap getSupportedMediaTypes() {
        final @NotNull MediaTypeMap map = new MediaTypeMap();
        //noinspection MagicNumber
        map.put(MediaType.MEDIA_TYPE_AUDIO_MPEG, Quality.valueOf(0.1));
        map.put(MediaType.MEDIA_TYPE_ANY, Quality.NOT_ACCEPTABLE);
        return map;
    }
}
