/*
 * Copyright 2019 nacamar GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ybrid.player.io;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * This implements a buffered {@link PCMDataSource} based on another such source.
 *
 * The purpose of this class is to provide a buffer for audio.
 */
public class AudioBuffer implements PCMDataSource {
    private static final int SLEEP_TIME = 371; /* [ms] */

    private final BlockingQueue<PCMDataBlock> buffer = new LinkedBlockingQueue<>();
    private double target;
    private PCMDataSource backend;
    private Consumer<PCMDataBlock> inputConsumer;
    private BufferThread thread = new BufferThread("AudioBuffer Buffer Thread");

    private class BufferThread extends Thread implements DataSource {
        private static final long POLL_TIMEOUT = 123; /* [ms] */
        private Exception exception = null;

        public BufferThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    if (getBufferLength() > target) {
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

            if (block == null)
                return;

            try {
                if (inputConsumer != null)
                    inputConsumer.accept(block);
            } catch (Exception e) {
                e.printStackTrace();
            }

            buffer.put(block);
        }

        private IOException toIOException(Exception e) {
            if (e instanceof IOException)
                return (IOException)e;

            return new IOException(e);
        }

        @Override
        public PCMDataBlock read() throws IOException {
            try {
                PCMDataBlock block = buffer.poll();

                if (block != null)
                    return block;

                do {
                    if (exception != null) {
                        throw toIOException(exception);
                    }
                    block = buffer.poll(POLL_TIMEOUT, TimeUnit.MILLISECONDS);
                } while (block == null);

                return block;
            } catch (InterruptedException e) {
                throw toIOException(e);
            }
        }

        @Override
        public boolean isValid() {
            return exception == null;
        }

        @Override
        public void close() throws IOException {
            interrupt();
        }
    }

    /**
     * Create an instance.
     *
     * @param target The amount of audio to be buffered in [s].
     * @param backend The backend to use.
     * @param inputConsumer A {@link Consumer} that is called when a new block is read into the buffer.
     */
    public AudioBuffer(double target, PCMDataSource backend, Consumer<PCMDataBlock> inputConsumer) {
        this.target = target;
        this.backend = backend;
        this.inputConsumer = inputConsumer;

        thread.start();
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

        return ret;
    }

    @Override
    public PCMDataBlock read() throws IOException {
        return thread.read();
    }

    @Override
    public boolean isValid() {
        return thread != null && thread.isValid();
    }

    @Override
    public void close() throws IOException {
        thread.close();
        thread = null;
        backend.close();
    }
}
