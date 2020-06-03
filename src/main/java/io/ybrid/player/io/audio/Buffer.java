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

import io.ybrid.player.io.DataSource;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.PCMDataSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This implements a buffered {@link PCMDataSource} based on another such source.
 *
 * The purpose of this class is to provide a buffer for audio.
 */
public class Buffer implements PCMDataSource, BufferStatusProvider {
    private final BufferThread thread;

    private static class Status implements BufferStatusProvider {
        private static final Duration MINIMUM_BETWEEN_ANNOUNCE = Duration.ofMillis(1000);

        final List<BufferStatusConsumer> consumers = new ArrayList<>();
        private Instant lastAnnounce = null;
        private long underruns = 0;
        private Instant underrunTimestamp = null;
        private long overruns = 0;
        private Instant overrunTimestamp = null;
        private double max = 0;
        private Instant maxTimestamp = null;
        private double minAfterMax = 0;
        private Instant minAfterMaxTimestamp = null;
        private double current = 0;
        private Instant currentTimestamp = null;

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

        private void setCurrent(double current) {
            Instant now = Instant.now();
            boolean forceAnnounce = false;

            this.current = current;
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

            status = new BufferStatus(underruns, underrunTimestamp, overruns, overrunTimestamp, max, maxTimestamp, minAfterMax, minAfterMaxTimestamp, current, currentTimestamp);

            synchronized (consumers) {
                for (BufferStatusConsumer consumer : consumers)
                    consumer.onBufferStatusUpdate(status);
            }

            lastAnnounce = now;
        }

        @Override
        public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
            synchronized (consumers) {
                if (!consumers.contains(consumer)) {
                    consumers.add(consumer);
                    lastAnnounce = null; // force next announce
                }
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
        private final Status state = new Status();
        @NotNull private final PCMDataSource backend;
        private final Consumer<PCMDataBlock> inputConsumer;
        private Exception exception = null;
        private double target;

        public BufferThread(String name, @NotNull PCMDataSource backend, Consumer<PCMDataBlock> inputConsumer, double target) {
            super(name);
            this.backend = backend;
            this.inputConsumer = inputConsumer;
            this.target = target;
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
            PCMDataBlock block = backend.read();

            try {
                if (inputConsumer != null)
                    inputConsumer.accept(block);
            } catch (Exception ignored) {
            }

            buffer.put(block);
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
                    getBufferLength(); // Update state.
                    return block;
                }

                state.underrun();

                do {
                    if (exception != null) {
                        throw toIOException(exception);
                    }
                    block = buffer.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
                } while (block == null);

                getBufferLength(); // Update state.
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
            double ret = 0;

            for (PCMDataBlock block : buffer) {
                ret += block.getBlockLength();
            }

            state.setCurrent(ret);

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
            return exception == null;
        }

        @Override
        public void close() throws IOException {
            interrupt();
            backend.close();
        }
    }

    /**
     * Create an instance.
     *
     * @param target The amount of audio to be buffered in [s].
     * @param backend The backend to use.
     * @param inputConsumer A {@link Consumer} that is called when a new block is read into the buffer.
     */
    public Buffer(double target, @NotNull PCMDataSource backend, Consumer<PCMDataBlock> inputConsumer) {
        thread = new BufferThread("Audio Buffer Thread", backend, inputConsumer, target); //NON-NLS
        thread.start();
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
}
