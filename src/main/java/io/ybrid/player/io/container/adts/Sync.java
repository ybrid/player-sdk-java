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

import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.FilterDataSource;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * This class implements a frame syncer for ADTS streams.
 *
 * Reading from this results in reading full valid frames.
 * Any invalid data between frames is discarded.
 */
public class Sync extends FilterDataSource<ByteDataSource> {
    static final @NonNls Logger LOGGER = Logger.getLogger(Sync.class.getName());

    private final @NotNull List<ByteDataBlock> queue = new ArrayList<>();

    /**
     * The main constructor.
     *
     * @param backend The backend to use.
     */
    public Sync(@NotNull ByteDataSource backend) {
        super(backend);
    }

    private void fill(int minimumLength) throws IOException {
        int have = 0;

        for (final @NotNull ByteDataBlock block : queue) {
            have += block.getData().length;
            if (have >= minimumLength)
                return;
        }

        while (have < minimumLength) {
            final @NotNull ByteDataBlock block = backend.read();
            queue.add(block);
            have += block.getData().length;
        }
    }

    private byte @NotNull [] requestArray(int length) throws IOException {
        final byte[] n = new byte[length];
        int have = 0;

        fill(length);

        for (final @NotNull ByteDataBlock block : queue) {
            int blockLength = block.getData().length;
            int todo = length - have;

            if (todo > blockLength)
                todo = blockLength;

            System.arraycopy(block.getData(), 0, n, have, todo);
            have += todo;

            if (have < 1)
                break;
        }

        return n;
    }

    private @NotNull ByteDataBlock requestBlock(int length) throws IOException {
        final @NotNull ByteDataBlock first = queue.get(0);
        return new ByteDataBlock(first.getSync(), first.getPlayoutInfo(), requestArray(length));
    }

    private void skip(int length) throws IOException {
        fill(length);

        while (length > 0) {
            final @NotNull ByteDataBlock block = queue.get(0);
            int blockLength = block.getData().length;

            if (blockLength <= length) {
                queue.remove(0);
                length -= blockLength;
            } else {
                int left = blockLength - length;
                byte[] n = new byte[left];
                System.arraycopy(block.getData(), length, n, 0, left);
                queue.set(0, new ByteDataBlock(block.getSync(), block.getPlayoutInfo(), n));
                return;
            }
        }
    }

    private void skipToSyncSync() throws IOException {
        int skip = 0;

        fill(1);

        for (final @NotNull ByteDataBlock block : queue) {
            final byte[] data = block.getData();
            int blockLength = data.length;
            int i;

            for (i = 0; i < blockLength; i++) {
                //noinspection MagicNumber
                if ((data[i] & 0xFF) == 0xFF) {
                    skip += i;
                    if (skip != 0) {
                        LOGGER.info("Skipping " + skip + " bytes to next potential sync word.");
                        skip(skip);
                    }
                    return;
                }
            }

            skip += blockLength;
        }

        LOGGER.info("Skipping " + skip + " bytes.");
        skip(skip);
    }

    @Override
    public @NotNull Frame read() throws IOException {
        Frame frame = null;
        skipToSyncSync();

        while (frame == null) {
            try {
                final @NotNull Header header = new Header(requestArray(9));

                frame = Frame.parse(requestBlock(header.getFrameLength()), header);
            } catch (Throwable e) {
                LOGGER.info("Failed to parse frame, skipping 1 byte and re-syncing: " + e);
                skip(1);
                skipToSyncSync();
            }
        }

        skip(frame.getHeader().getFrameLength());

        return frame;
    }

    @Override
    public void mark() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }
}
