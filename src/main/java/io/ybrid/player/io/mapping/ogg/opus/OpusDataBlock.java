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

import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.codec.opus.TableOfContents;
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NotNull;

public class OpusDataBlock extends ByteDataBlock {
    private final @NotNull OpusHead opusHead;
    private final @NotNull PacketAdapter packet;
    private final @NotNull TableOfContents tableOfContents;

    public OpusDataBlock(@NotNull OpusHead opusHead, @NotNull PacketAdapter packet) {
        super(packet.getSync(), packet.getPlayoutInfo(), packet.getData());
        this.opusHead = opusHead;
        this.packet = packet;
        this.tableOfContents = new TableOfContents(packet.getData(), 0);
    }

    /**
     * Gets the {@link TableOfContents} for the Opus frame contained within.
     * @return The {@link TableOfContents}.
     */
    public @NotNull TableOfContents getTableOfContents() {
        return tableOfContents;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "OpusDataBlock{" +
                "opusHead=" + opusHead +
                ", packet=" + packet +
                ", tableOfContents=" + tableOfContents +
                "}";
    }
}
