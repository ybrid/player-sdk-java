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
import io.ybrid.api.transaction.Transaction;
import io.ybrid.player.io.*;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class BufferMuxer implements PCMDataSource, BufferStatusProvider, BufferStatusConsumer {
    @NonNls
    static final Logger LOGGER = Logger.getLogger(BufferMuxer.class.getName());
    private static final double AUDIO_BUFFER_TARGET = 10; /* [s] */

    private interface DataBlockConsumer {
        void blockAccept(@NotNull DataBlock dataBlock, @NotNull Entry entry);
    }

    private static class Entry {
        private final @NotNull Buffer buffer;
        private final @NotNull Transaction transaction;

        public Entry(@NotNull PCMDataSource source, @NotNull DataBlockConsumer consumer, @NotNull Transaction transaction) {
            this.transaction = transaction;
            this.buffer = new Buffer(AUDIO_BUFFER_TARGET, source, dataBlock -> consumer.blockAccept(dataBlock, this));
        }

        public @NotNull Buffer getBuffer() {
            return buffer;
        }

        public @NotNull PCMDataBlock read() throws IOException {
            final @NotNull PCMDataBlock block = buffer.read();
            final @Nullable Runnable onAudible = block.getOnAudible();

            if (onAudible == null) {
                block.setOnAudible(transaction::setAudioComplete);
            } else {
                block.setOnAudible(() -> {onAudible.run(); transaction.setAudioComplete();});
            }
            return block;
        }

        public boolean isValid() {
            return buffer.isValid();
        }

        @Override
        public String toString() {
            //noinspection HardCodedStringLiteral
            return "Entry{" +
                    "buffer=" + buffer + " (isValid: " + buffer.isValid() + ")" +
                    "}";
        }
    }

    private static class Callback implements Runnable {
        private @Nullable Runnable callback = null;
        private boolean fired = false;
        private boolean running = false;

        public synchronized void setCallback(@Nullable Runnable callback) {
            this.callback = callback;
        }

        public synchronized boolean isSet() {
            return callback != null;
        }

        public synchronized void recover () {
            fired = false;
        }

        @Override
        public void run() {
            final @NotNull Runnable runnable;

            synchronized (this) {
                if (fired || running || callback == null)
                    return;

                fired = true;
                running = true;
                runnable = callback;
            }

            new Thread(() -> {
                try {
                    runnable.run();
                } catch (Throwable ignored) {
                }
                synchronized (Callback.this) {
                    running = false;
                }
            }).start();
        }
    }

    private final @NotNull Set<BufferStatusConsumer> consumers = new HashSet<>();
    private final @NotNull DataBlockMetadataUpdateThread metadataUpdateThread;
    private final @NotNull List<Entry> buffers = new ArrayList<>();
    private Entry selectedBuffer;
    private @Nullable BufferStatus lastBufferStatus = null;
    private final @NotNull Object callbackLock = new Object();
    private final @NotNull Callback inputEOFCallback = new Callback();

    public BufferMuxer(@NotNull Session session) {
        metadataUpdateThread = new DataBlockMetadataUpdateThread("Main Metadata Update Thread", session);
        metadataUpdateThread.start();
    }

    public void addBuffer(@NotNull PCMDataSource source, @NotNull Transaction transaction) {
        final @NotNull Entry newEntry = new Entry(source, ((dataBlock, entry) -> {
            if (entry == selectedBuffer)
                metadataUpdateThread.accept(dataBlock);
        }), transaction);

        newEntry.getBuffer().addBufferStatusConsumer(status -> {
            if (newEntry == selectedBuffer)
                onBufferStatusUpdate(status);
        });

        LOGGER.info("Adding Entry: " + newEntry);
        synchronized (buffers) {
            buffers.add(newEntry);
        }
    }

    private void selectNext() {
        LOGGER.info("Selecting new entry...");
        synchronized (buffers) {
            for (final @NotNull Iterator<Entry> iterator = buffers.iterator(); iterator.hasNext(); ) {
                final @NotNull Entry entry = iterator.next();

                if (!entry.isValid()) {
                    try {
                        entry.getBuffer().close();
                    } catch (IOException ignored) {
                    }
                    iterator.remove();
                    continue;
                }

                if (entry == selectedBuffer)
                    continue;

                LOGGER.info("Selected: " + entry);
                selectedBuffer = entry;
                return;
            }

            // No valid buffer found.
            LOGGER.info("Selected: <null>");
            selectedBuffer = null;
        }
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        synchronized (buffers) {
            if (selectedBuffer == null || !selectedBuffer.isValid()) {
                LOGGER.info("Buffer is invalid, selecting a new one.");
                selectNext();
                if (selectedBuffer == null || !selectedBuffer.isValid()) {
                    LOGGER.info("Buffer is still invalid. Throwing error.");
                    throw new IOException("No valid Buffer");
                }
            }
            try {
                synchronized (callbackLock) {
                    if (inputEOFCallback.isSet()) {
                        if (selectedBuffer.getBuffer().hasInputReachedEOF()) {
                            inputEOFCallback.run();
                        } else {
                            inputEOFCallback.recover();
                        }
                    }
                }
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

    public boolean isInHandover() {
        synchronized (buffers) {
            if (!selectedBuffer.getBuffer().hasInputReachedEOF())
                return false;

            for (final @NotNull Entry entry : buffers) {
                if (entry == selectedBuffer)
                    continue;

                if (entry.isValid())
                    return true;
            }
        }
        return false;
    }

    public void setInputEOFCallback(@Nullable Runnable inputEOFCallback) {
        synchronized (callbackLock) {
            this.inputEOFCallback.setCallback(inputEOFCallback);
        }
    }

    @Override
    public void close() throws IOException {
        setInputEOFCallback(null);
        synchronized (buffers) {
            for (final @NotNull Entry entry : buffers)
                entry.getBuffer().close();
            buffers.clear();
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
