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

package io.ybrid.player.io.muxer.ogg;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.source.Source;
import io.ybrid.api.metadata.source.SourceType;
import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.container.ogg.Flag;
import io.ybrid.player.io.container.ogg.Page;
import io.ybrid.player.io.container.ogg.Sync;
import io.ybrid.player.io.mapping.ogg.Generic;
import io.ybrid.player.io.mapping.ogg.opus.Mapping;
import io.ybrid.player.io.muxer.StreamInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Demuxer extends io.ybrid.player.io.muxer.Demuxer<Stream, PacketAdapter> {
    @SuppressWarnings("unchecked")
    private final Class<? extends Generic>[] mappings = new Class[]{Mapping.class};
    private final @NotNull Sync sync = new Sync();
    private final Map<Integer, @Nullable Stream> streams = new HashMap<>();
    private io.ybrid.api.metadata.Sync blockSync = new io.ybrid.api.metadata.Sync.Builder(new Source(SourceType.FORMAT)).build();
    private PlayoutInfo blockPlayoutInfo = null;

    private void handle(@NotNull Page page) {
        final @NotNull Set<Flag> flags = page.getFlags();
        final int serial = page.getSerial();
        @Nullable PageAdapter block;
        final @Nullable Stream stream;

        if (flags.contains(Flag.BOS)) {
            @Nullable StreamInfo streamInfo = null;

            for (final @NotNull Class<? extends Generic> mapping : mappings) {
                try {
                    streamInfo = (StreamInfo) mapping.getMethod("test", Page.class).invoke(null, page);
                    if (streamInfo != null)
                        break;
                } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
                }
            }

            if (streamInfo != null) {
                if (runPredicate(isWantedCallback, streamInfo, false)) {
                    stream = new Stream(streamInfo, this);
                    streams.put(serial, stream);
                    runConsumer(onBeginOfStreamCallback, stream);
                } else {
                    stream = null;
                }
            } else {
                stream = null;
            }
        } else {
            stream = streams.get(serial);
        }

        if (stream == null)
            return;

        block = new PageAdapter(blockSync, blockPlayoutInfo, page);

        stream.consume(block);

        if (flags.contains(Flag.EOS)) {
            runConsumer(onEndOfStreamCallback, stream);
            streams.remove(serial);
        }
    }

    @Override
    public void fill(@NotNull ByteDataBlock block) {
        blockPlayoutInfo = block.getPlayoutInfo();
        blockSync = block.getSync();
        sync.fill(block.getData(), 0, block.getData().length);
    }

    @Override
    public void iter() throws IOException {
        @Nullable Page page;

        while ((page = sync.read()) != null)
            handle(page);

        autofill();

        while ((page = sync.read()) != null)
            handle(page);
    }
}
