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

import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.container.ogg.Packet;
import io.ybrid.player.io.mapping.Header;
import io.ybrid.player.io.mapping.Mapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class Stream extends io.ybrid.player.io.muxer.Stream<Stream, Header, DataBlock, DataBlock> {
    private io.ybrid.player.io.container.ogg.Stream stream;

    public void consume(@NotNull PageAdapter block) {
        @Nullable Packet packet;

        if (stream == null) {
            stream = new io.ybrid.player.io.container.ogg.Stream(block.getPage());
        } else {
            stream.add(block.getPage());
        }

        while ((packet = stream.read()) != null) {
            consume(new PacketAdapter(block.getSync(), block.getPlayoutInfo(), packet));
        }
    }

    public Stream(@NotNull Mapping<DataBlock, ?> mapping, @NotNull Demuxer demuxer) {
        super(mapping, demuxer);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void close() throws IOException {
        // TODO
    }
}
