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

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NotNull;

abstract class Header extends io.ybrid.player.io.mapping.Header {
    /**
     * Create a new DataBlock.
     *
     * @param sync        The {@link Sync} to use for the new DataBlock.
     * @param playoutInfo The {@link PlayoutInfo} to use for the new DataBlock.
     */
    protected Header(@NotNull Sync sync, PlayoutInfo playoutInfo) {
        super(sync, playoutInfo);
    }

    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    static boolean isHeader(@NotNull PacketAdapter block, @NotNull byte[] magic) {
        final @NotNull byte[] data = block.getData();

        if (data.length < magic.length)
            return false;

        for (int i = 0; i < magic.length; i++) {
            if (data[i] != magic[i])
                return false;
        }

        return true;
    }
}
