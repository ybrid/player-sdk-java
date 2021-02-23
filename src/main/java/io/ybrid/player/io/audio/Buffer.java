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

import io.ybrid.api.util.Identifier;
import io.ybrid.api.util.hasIdentifier;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.DataSource;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.PCMDataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This implements a buffered {@link PCMDataSource} based on another such source.
 *
 * The purpose of this class is to provide a buffer for audio.
 */
public class Buffer implements PCMDataSource, BufferStatusProvider, hasIdentifier {
    private static final String AUDIO_BUFFER_THREAD_NAME = "Audio Buffer Thread"; //NON-NLS

    private final BufferThread thread;
    private @NotNull Identifier identifier;

    private static class Status implements BufferStatusProvider {
        private static final Duration MINIMUM_BETWEEN_ANNOUNCE = Duration.ofMillis(1000);

        private final Set<BufferStatusConsumer> consumers = new HashSet<>();
        private final @NotNull Buffer buffer;
        private @Nullable Instant lastAnnounce = null;
        private long underruns = 0;
        private @Nullable Instant underrunTimestamp = null;
        private long overruns = 0;
        private @Nullable Instant overrunTimestamp = null;
        private double max = 0;
        private @Nullable Instant maxTimestamp = null;
        private double minAfterMax = 0;
        private @Nullable Instant minAfterMaxTimestamp = null;
        private double current = 0;
        private @Nullable Instant currentTimestamp = null;
        private long samplesRead;
        private long samplesForwarded;

        private Status(@NotNull Buffer buffer) {
            this.buffer = buffer;
        }

        private void underrun() {
            underruns++;
            underrunTimestamp = Instant.now();
            announce(true, underrunTimestamp);
        }

        private void overrun() {
            overruns++;
            overrunTimestamp = Instant.now();
            announce(true, overrunTimestamp);
        }

        private void setCurrent(double current, long samplesRead, long samplesForwarded) {
            Instant now = Instant.now();
            boolean forceAnnounce = false;

            this.current = current;
            this.samplesRead = samplesRead;
            this.samplesForwarded = samplesForwarded;
            currentTimestamp = now;

            if (current > max) {
                max = current;
                maxTimestamp = now;
                minAfterMax = current;
                minAfterMaxTimestamp = now;
                forceAnnounce = true;
            } else if (current < minAfterMax) {
                minAfterMax = current;
                minAfterMaxTimestamp = now;
                forceAnnounce = true;
            }

            announce(forceAnnounce, now);
        }

        private synchronized void announce(boolean force, @NotNull final Instant now) {
            final BufferStatus status;

            if (consumers.isEmpty())
                return;

            if (!force && lastAnnounce != null && lastAnnounce.plus(MINIMUM_BETWEEN_ANNOUNCE).isAfter(now))
                return;

            status = new BufferStatus(buffer.getIdentifier(), underruns, underrunTimestamp, overruns, overrunTimestamp, max, maxTimestamp, minAfterMax, minAfterMaxTimestamp, current, currentTimestamp, samplesRead, samplesForwarded);

            synchronized (consumers) {
                for (BufferStatusConsumer consumer : consumers)
                    consumer.onBufferStatusUpdate(status);
            }

            lastAnnounce = now;
        }

        @Override
        public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
            synchronized (consumers) {
                if (consumers.add(consumer))
                    lastAnnounce = null; // force next announce
            }
        }

        @Override
        public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
            synchronized (consumers) {
                consumers.remove(consumer);
            }
        }
    }

    private static class BufferThread extends Thread implements DataSource, BufferStatusProvider {
        private static final long POLL_TIMEOUT = 123; /* [ms] */
        private static final int SLEEP_TIME = 371; /* [ms] */

        private final BlockingQueue<PCMDataBlock> buffer = new LinkedBlockingQueue<>();
        private final Status state;
        @NotNull private final PCMDataSource backend;
        private final Consumer<DataBlock> inputConsumer;
        private Exception exception = null;
        private double target;
        private long samplesRead = 0;
        private long samplesForwarded = 0;

        public BufferThread(String name, @NotNull Buffer buffer, @NotNull PCMDataSource backend, Consumer<DataBlock> inputConsumer, double target) {
            super(name);
            this.backend = backend;
            this.inputConsumer = inputConsumer;
            this.target = target;
            this.state = new Status(buffer);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    if (getBufferLength() > target) {
                        state.overrun();
                        //noinspection BusyWait
                        sleep(SLEEP_TIME);
                    } else {
                        pump();
                    }
                }
            } catch (InterruptedException | IOException e) {
                exception = e;
            }
        }

        private void pump() throws IOException, InterruptedException {
            final @NotNull PCMDataBlock block;

            try {
                block = backend.read();
            } catch (RuntimeException e) {
                throw new IOException(e);
            }

            try {
                if (inputConsumer != null)
                    inputConsumer.accept(block);
            } catch (Exception ignored) {
            }

            buffer.put(block);
            samplesRead += block.getData().length;
        }

        private IOException toIOException(Exception e) {
            if (e instanceof IOException)
                return (IOException)e;

            return new IOException(e);
        }

        @Override
        public @NotNull PCMDataBlock read() throws IOException {
            try {
                PCMDataBlock block = buffer.poll();

                if (block != null) {
                    // Update state.
                    getBufferLength();
                    samplesForwarded += block.getData().length;

                    return block;
                }

                state.underrun();

                do {
                    if (exception != null) {
                        throw toIOException(exception);
                    }
                    block = buffer.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
                } while (block == null);

                // Update state.
                getBufferLength();
                samplesForwarded += block.getData().length;

                return block;
            } catch (InterruptedException e) {
                throw toIOException(e);
            }
        }

        /**
         * Returns the fullness of the buffer.
         * @return The fullness in [s].
         */
        public double getBufferLength() {
            final long skipped = backend.getSkippedSamples();
            double ret = 0;

            for (PCMDataBlock block : buffer) {
                ret += block.getBlockLength();
            }

            state.setCurrent(ret, samplesRead + skipped, samplesForwarded > 0 ? samplesForwarded + skipped : 0);

            return ret;
        }

        @Override
        public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
            state.addBufferStatusConsumer(consumer);
        }

        @Override
        public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
            state.removeBufferStatusConsumer(consumer);
        }

        @Override
        public boolean isValid() {
            return !buffer.isEmpty() || exception == null;
        }

        @Override
        public void close() throws IOException {
            interrupt();
            backend.close();
        }

        public boolean hasInputReachedEOF() {
            return exception != null;
        }
    }

    /**
     * Create an instance.
     *
     * @param target The amount of audio to be buffered in [s].
     * @param backend The backend to use.
     * @param inputConsumer A {@link Consumer} that is called when a new block is read into the buffer.
     */
    public Buffer(double target, @NotNull PCMDataSource backend, Consumer<DataBlock> inputConsumer) {
        this(new Identifier(), target, backend, inputConsumer);
    }

    /**
     * Create an instance.
     *
     * @param identifier The identifier for this buffer.
     * @param target The amount of audio to be buffered in [s].
     * @param backend The backend to use.
     * @param inputConsumer A {@link Consumer} that is called when a new block is read into the buffer.
     */
    public Buffer(@Nullable Identifier identifier, double target, @NotNull PCMDataSource backend, Consumer<DataBlock> inputConsumer) {
        setIdentifier(identifier);
        thread = new BufferThread(AUDIO_BUFFER_THREAD_NAME, this, backend, inputConsumer, target);
        thread.start();
    }

    /**
     * Gets the identifier for this buffer.
     * @return The identifier.
     */
    @Override
    public @NotNull Identifier getIdentifier() {
        return identifier;
    }

    /**
     * Sets the identifier for the buffer.
     * The identifier can only be set to non-{@code null} once.
     *
     * @param identifier The identifier to set to.
     */
    public void setIdentifier(@Nullable Identifier identifier) {
        if (this.identifier != null && !this.identifier.equals(identifier))
            throw new IllegalArgumentException("Identifier can not be updated (while trying to update from " + this.identifier + " to " + identifier + ")");
        this.identifier = identifier;
    }

    @Override
    public @NotNull PCMDataBlock read() throws IOException {
        return thread.read();
    }

    @Override
    public boolean isValid() {
        return thread.isValid();
    }

    @Override
    public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        thread.addBufferStatusConsumer(consumer);
    }

    @Override
    public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        thread.removeBufferStatusConsumer(consumer);
    }

    @Override
    public void close() throws IOException {
        thread.close();
    }


    /**
     * Gets whether the input side has reached EOF.
     * @return Whether input reached EOF.
     */
    public boolean hasInputReachedEOF() {
        return thread.hasInputReachedEOF();
    }
}
