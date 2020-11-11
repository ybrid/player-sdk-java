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

package io.ybrid.player.io.mapping.ogg.opus;

import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.MediaType;
import io.ybrid.player.io.container.ogg.Page;
import io.ybrid.player.io.mapping.Header;
import io.ybrid.player.io.mapping.ogg.Generic;
import io.ybrid.player.io.muxer.StreamInfo;
import io.ybrid.player.io.muxer.StreamUsage;
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.Set;

public class Mapping extends Generic {
    @NonNls
    private static final byte[] MAGIC = "OpusHead".getBytes(StandardCharsets.UTF_8);

    @Override
    public @NotNull DataBlock process(@NotNull PacketAdapter block) {
        final @NotNull byte[] data = block.getData();
        if (data.length > 2 && data[0] == 'O' && data[1] == 'p') {
            return new Header(block.getSync(), block.getPlayoutInfo()) {};
        } else {
            return block;
        }
    }

    @Override
    public @NotNull StreamUsage getPrimaryStreamUsage() {
        return StreamUsage.AUDIO;
    }

    @Override
    public @NotNull Set<StreamUsage> getStreamUsage() {
        return EnumSet.of(StreamUsage.AUDIO, StreamUsage.METADATA);
    }

    public static @Nullable StreamInfo test(@NotNull Page page) {
        if (page.getBody().length > 8 && page.bodyContains(0, MAGIC)) {
            return new StreamInfo(new Mapping());
        }
        return null;
    }

    @Override
    public @NotNull String getContentType() {
        return MediaType.BLOCK_STREAM_OPUS;
    }
}
