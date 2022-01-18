/*
 * Copyright (c) 2022 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.container.adts;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.metadata.Sync;
import io.ybrid.player.io.ByteDataBlock;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Frame extends ByteDataBlock {
    private final @NotNull Header header;

    private Frame(@NotNull Sync sync, PlayoutInfo playoutInfo, byte[] data, @NotNull Header header) {
        super(sync, playoutInfo, data);
        this.header = header;
    }

    @Contract("_, _, _ -> new")
    public static @NotNull Frame parse(@NotNull ByteDataBlock block, @NotNull Header header) {
        return new Frame(block.getSync(), block.getPlayoutInfo(), block.getData(), header);
    }

    public @NotNull Header getHeader() {
        return header;
    }

    @Override
    public String toString() {
        return "Frame{" +
                "data.length=" + data.length +
                ", header=" + header +
                '}';
    }
}
