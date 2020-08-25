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

package io.ybrid.player.io.audio;

import io.ybrid.api.Session;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.DataBlockMetadataUpdateThread;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.PCMDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

public class BufferMuxer implements PCMDataSource, BufferStatusProvider, BufferStatusConsumer {
    private static final double AUDIO_BUFFER_TARGET = 10; /* [s] */

    private interface DataBlockConsumer {
        void blockAccept(@NotNull DataBlock dataBlock, @NotNull Entry entry);
    }

    private static class Entry {
        private final @NotNull Buffer buffer;

        public Entry(@NotNull PCMDataSource source, @NotNull DataBlockConsumer consumer) {
            this.buffer = new Buffer(AUDIO_BUFFER_TARGET, source, (dataBlock -> consumer.blockAccept(dataBlock, this)));
        }

        public @NotNull Buffer getBuffer() {
            return buffer;
        }

        public @NotNull PCMDataBlock read() throws IOException {
            return buffer.read();
        }

        public boolean isValid() {
            return buffer.isValid();
        }
    }

    private final @NotNull Set<BufferStatusConsumer> consumers = new HashSet<>();
    private final @NotNull DataBlockMetadataUpdateThread metadataUpdateThread;
    private final @NotNull List<Entry> buffers = new ArrayList<>();
    private Entry selectedBuffer;
    private @Nullable BufferStatus lastBufferStatus = null;

    public BufferMuxer(@NotNull Session session) {
        metadataUpdateThread = new DataBlockMetadataUpdateThread("Main Metadata Update Thread", session);
        metadataUpdateThread.start();
    }

    public void addBuffer(@NotNull PCMDataSource source) {
        final @NotNull Entry newEntry = new Entry(source, ((dataBlock, entry) -> {
            if (entry == selectedBuffer)
                metadataUpdateThread.accept(dataBlock);
        }));

        newEntry.getBuffer().addBufferStatusConsumer(status -> {
            if (newEntry == selectedBuffer)
                onBufferStatusUpdate(status);
        });

        synchronized (buffers) {
            buffers.add(newEntry);
        }
    }

    private void selectNext() {
        synchronized (buffers) {
            for (final @NotNull Iterator<Entry> iterator = buffers.iterator(); iterator.hasNext(); ) {
                final @NotNull Entry entry = iterator.next();

                if (!entry.isValid()) {
                    iterator.remove();
                    continue;
                }

                if (entry == selectedBuffer)
                    continue;

                selectedBuffer = entry;
                return;
            }

            // No valid buffer found.
            selectedBuffer = null;
        }
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        synchronized (buffers) {
            try {
                return selectedBuffer.read();
            } catch (Exception e) {
                selectNext();
                return selectedBuffer.read();
            }
        }
    }

    @Override
    public boolean isValid() {
        synchronized (buffers) {
            for (final @NotNull Entry entry : buffers) {
                if (entry.isValid())
                    return true;
            }
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        synchronized (buffers) {
            for (final @NotNull Entry entry : buffers)
                entry.getBuffer().close();
        }
        metadataUpdateThread.interrupt();
    }

    @Override
    public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        synchronized (consumers) {
            if (consumers.add(consumer)) {
                if (lastBufferStatus != null)
                    consumer.onBufferStatusUpdate(lastBufferStatus);
            }
        }
    }

    @Override
    public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        synchronized (consumers) {
            consumers.remove(consumer);
        }
    }

    @Override
    public void onBufferStatusUpdate(@NotNull BufferStatus status) {
        synchronized (consumers) {
            lastBufferStatus = status;
            for (BufferStatusConsumer consumer : consumers)
                consumer.onBufferStatusUpdate(status);
        }
    }
}
