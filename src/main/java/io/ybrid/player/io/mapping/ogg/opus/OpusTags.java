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

import io.ybrid.player.io.container.ogg.Util;
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.charset.StandardCharsets;
import java.util.*;

class OpusTags extends Header {
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    @NonNls
    static final byte[] MAGIC = "OpusTags".getBytes(StandardCharsets.UTF_8);

    private final @NotNull PacketAdapter block;
    private final @NotNull String vendorString;
    private final @NotNull Map<@NotNull String, @NotNull List<@NotNull String>> comments = new HashMap<>();

    private int processComment(@NotNull byte[] raw, int offset) {
        final int len = Util.readLE32(raw, offset);
        final @NotNull String key;
        final @NotNull String value;
        int separatorOffset = -1;

        offset += 4;

        for (int i = offset; i < (offset + len); i++) {
            //noinspection MagicCharacter
            if (raw[i] == '=') {
                separatorOffset = i;
                break;
            }
        }

        if (separatorOffset < 1)
            throw new IllegalArgumentException("Malformed comment in OpusTags header");

        key = new String(Util.extractBytes(raw, offset, separatorOffset - offset), StandardCharsets.UTF_8).toUpperCase(Locale.ROOT);
        value = new String(Util.extractBytes(raw, separatorOffset + 1, len - 1 - (separatorOffset - offset)), StandardCharsets.UTF_8);

        if (!comments.containsKey(key))
            comments.put(key, new ArrayList<>(1));

        comments.get(key).add(value);

        offset += len;

        return offset;
    }

    public OpusTags(@NotNull PacketAdapter block) {
        super(block.getSync(), block.getPlayoutInfo());
        final @NotNull byte[] raw = block.getData();
        int offset = 8;
        int commentCount;
        int len;

        this.block = block;

        len = Util.readLE32(raw, offset);
        offset += 4;
        this.vendorString = new String(Util.extractBytes(raw, offset, len), StandardCharsets.UTF_8);
        offset += len;

        commentCount = Util.readLE32(raw, offset);
        offset += 4;

        for (int i = 0; i < commentCount; i++) {
            offset = processComment(raw, offset);
        }

        if (offset != raw.length)
            throw new IllegalArgumentException("Malformed OpusTags header");
    }

    public @NotNull byte[] getRaw() {
        return block.getData();
    }

    public @NotNull String getVendorString() {
        return vendorString;
    }

    @UnmodifiableView
    public @NotNull Map<String, List<String>> getComments() {
        return Collections.unmodifiableMap(comments);
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "OpusTags{" +
                "vendorString='" + vendorString + "'" +
                ", comments=" + comments +
                "}";
    }
}
