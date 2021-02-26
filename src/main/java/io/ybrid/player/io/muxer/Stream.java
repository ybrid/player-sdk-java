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

package io.ybrid.player.io.muxer;

import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.DataSource;
import io.ybrid.player.io.mapping.Header;
import io.ybrid.player.io.mapping.Mapping;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.io.EOFException;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public abstract class Stream<T extends Stream<T, H, D, I>, H extends Header, D extends DataBlock, I extends DataBlock> implements DataSource {
    protected final @NotNull List<@NotNull H> headers = new ArrayList<>();
    protected final @NotNull Queue<D> readyPackets = new LinkedList<>();
    protected final @NotNull StreamInfo streamInfo;
    protected final @NotNull Demuxer<T, I> demuxer;
    protected boolean headersComplete = false;
    protected boolean reachedEOF = false;
    protected @Nullable Consumer<@NotNull T> onBeginOfStreamCallback;
    protected @Nullable Consumer<@NotNull T> onEndOfStreamCallback;
    protected @Nullable Consumer<@NotNull T> onHeaderReadyCallback;
    protected @Nullable Consumer<@NotNull T> onHeadersCompleteCallback;
    protected @Nullable Consumer<@NotNull T> onDataReadyCallback;

    private void runCallback(@Nullable Consumer<T> callback) {
        if (callback == null)
            return;

        try {
            //noinspection unchecked
            callback.accept((T) this);
        } catch (Throwable ignored) {
        }
    }

    protected Stream(@NotNull StreamInfo streamInfo, @NotNull Demuxer<T, I> demuxer) {
        this.streamInfo = streamInfo;
        this.demuxer = demuxer;
    }

    public void setOnBeginOfStreamCallback(@Nullable Consumer<T> onBeginOfStreamCallback) {
        this.onBeginOfStreamCallback = onBeginOfStreamCallback;
    }

    public void setOnEndOfStreamCallback(@Nullable Consumer<T> onEndOfStreamCallback) {
        this.onEndOfStreamCallback = onEndOfStreamCallback;
    }

    public void setOnHeaderReadyCallback(@Nullable Consumer<T> onHeaderReadyCallback) {
        this.onHeaderReadyCallback = onHeaderReadyCallback;
    }

    public void setOnHeadersCompleteCallback(@Nullable Consumer<T> onHeadersCompleteCallback) {
        this.onHeadersCompleteCallback = onHeadersCompleteCallback;
    }

    public void setOnDataReadyCallback(@Nullable Consumer<T> onDataReadyCallback) {
        this.onDataReadyCallback = onDataReadyCallback;
    }

    public @NotNull StreamInfo getStreamInfo() {
        return streamInfo;
    }

    public void consume(@NotNull I packet) {
        //noinspection unchecked
        final @NotNull DataBlock n = ((Mapping<I, ? extends DataBlock>)streamInfo.getMapping()).process(packet);
        try {
            //noinspection unchecked
            headers.add((H) n);
            runCallback(onHeaderReadyCallback);
        } catch (ClassCastException ignored) {
            //noinspection unchecked
            readyPackets.add((D)n);
            if (!headersComplete) {
                runCallback(onHeadersCompleteCallback);
                headersComplete = true;
            }
            runCallback(onDataReadyCallback);
        }
    }

    protected void signalEOF() {
        reachedEOF = true;
    }

    @UnmodifiableView
    public @NotNull List<? extends H> getHeaders() {
        return Collections.unmodifiableList(headers);
    }

    public @Nullable D read(boolean autofill) throws IOException {
        @Nullable D packet = readyPackets.poll();
        if (packet == null) {
            if (reachedEOF) {
                runCallback(onEndOfStreamCallback);
                throw new EOFException();
            }
            if (autofill) {
                demuxer.iter();
                packet = readyPackets.poll();
            }
        }
        return packet;
    }

    @Override
    public @NotNull D read() throws IOException {
        do {
            final @Nullable D block = read(true);
            if (block != null)
                return block;
            if (demuxer.isEofOnAutofill())
                throw new EOFException();
        } while (demuxer.canAutofill());
        throw new IllegalStateException("No data in queue and autofill not enabled.");
    }

    @Override
    public @Nullable String getContentType() {
        return streamInfo.getContentType();
    }
}
