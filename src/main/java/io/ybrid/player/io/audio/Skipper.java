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

package io.ybrid.player.io.audio;

import org.jetbrains.annotations.NotNull;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayDeque;

/**
 * The Skipper implements a {@link FilterPCMDataSource} that allows skipping frames
 * from the beginning, and the end of a stream.
 */
public abstract class Skipper<T extends PCMDataSource> extends FilterPCMDataSource<T> {
    private long read = 0;
    private long written = 0;
    private long skipped = 0;
    /**
     * Number of frames to skip at the beginning of the stream.
     */
    protected long preSkip = 0;
    /**
     * Number of frames to skip at the end of the stream.
     */
    protected long postSkip = 0;
    /**
     * Whether the pre-skip has completed.
     * This can be used to speed up checks in {@link #examine(PCMDataBlock)}.
     * <P>
     * Note: This value must not be written.
     */
    protected boolean preSkipDone = false;
    /**
     * Whether skipped frames should be accounted for in the return value of
     * {@link #getSkippedSamples()}.
     */
    protected boolean accountSkipped = true;
    private boolean reachedEOF = false;
    private final @NotNull ArrayDeque<PCMDataBlock> queue = new ArrayDeque<>();

    /**
     * To be implemented by implementing classes.
     * This inspects a block read from the input and updates
     * {@link #preSkip}, and {@link #postSkip}.
     * @param block The block read off the backend.
     */
    abstract protected void examine(@NotNull PCMDataBlock block);

    private @NotNull PCMDataBlock readFromBackend() throws IOException {
        try {
            final @NotNull PCMDataBlock block = backend.read();
            read += block.getLengthAsFrames();
            examine(block);
            return block;
        } catch (Throwable e) {
            reachedEOF = true;
            throw e;
        }
    }

    private @NotNull PCMDataBlock writeToFrontend(@NotNull PCMDataBlock block) {
        written += block.getLengthAsFrames();
        return block;
    }

    private void skipBlock(@NotNull PCMDataBlock block) {
        skipped += block.getLengthAsFrames();
    }

    /**
     * The main constructor.
     *
     * @param backend The backend to use.
     */
    public Skipper(@NotNull T backend) {
        super(backend);
    }

    private @NotNull PCMDataBlock readWithPreSkip() throws IOException {
        while (!preSkipDone) {
            final @NotNull PCMDataBlock block = readFromBackend();
            if (read <= preSkip) {
                skipBlock(block);
            } else if ((read - block.getLengthAsFrames()) == preSkip) {
                preSkipDone = true;
                return block;
            } else {
                preSkipDone = true;
                return block.subBlock(block.getLengthAsFrames() - (int)(read - preSkip), block.getLengthAsFrames());
            }
        }

        return readFromBackend();
    }

    private void fill() {
        while (!reachedEOF) {
            try {
                queue.add(readWithPreSkip());
            } catch (IOException e) {
                return;
            }

            if ((written + queue.element().getLengthAsFrames()) <= (read - preSkip - postSkip))
                break;
        }
    }

    private @NotNull PCMDataBlock readWithPostSkip() throws IOException {
        final @NotNull PCMDataBlock block;
        final long currentEndPosition;
        final int left;

        fill();
        if (queue.isEmpty() && reachedEOF)
            throw new EOFException();

        block = queue.remove();
        currentEndPosition = written + preSkip + block.getLengthAsFrames();

        if (currentEndPosition <= (read - postSkip))
            return block;

        // at this point we should have reached EOF, or we had a IO error.
        if (!reachedEOF)
            throw new IOException("Invalid queue state.");

        left = (int)((read - postSkip) - (written + preSkip));
        return block.subBlock(0, left);
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        return writeToFrontend(readWithPostSkip());
    }

    @Override
    public long getSkippedSamples() {
        if (accountSkipped) {
            return backend.getSkippedSamples() + skipped;
        } else {
            return backend.getSkippedSamples();
        }
    }

    @Override
    public void mark() {
        // TODO.
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        // TODO.
        throw new UnsupportedOperationException();
    }
}
