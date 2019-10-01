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

package io.ybrid.client.player.io;

import java.io.IOException;
import java.util.LinkedList;

public class AudioBuffer implements PCMDataSource {
    private static final int SLEEP_TIME = 371; /* [ms] */

    private final LinkedList<PCMDataBlock> buffer = new LinkedList<>();
    private double target;
    private PCMDataSource backend;
    private BufferThread thread = new BufferThread();

    private class BufferThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                if (getBufferLength() > target) {
                    try {
                        sleep(SLEEP_TIME);
                    } catch (InterruptedException e) {
                        return;
                    }
                } else {
                    pump();
                }
            }
        }

        private void pump() {
            PCMDataBlock block = backend.read();

            synchronized (buffer) {
                buffer.add(block);
            }
        }

        PCMDataBlock getBlock() {
            synchronized (buffer) {
                if (buffer.size() == 0) {
                    pump();
                }

                return buffer.remove();
            }
        }
    }

    public AudioBuffer(double target, PCMDataSource backend) {
        this.target = target;
        this.backend = backend;

        thread.start();
    }

    public double getBufferLength() {
        double ret = 0;

        synchronized (buffer) {
            for (PCMDataBlock block : buffer) {
                ret += block.getBlockLength();
            }
        }

        return ret;
    }

    @Override
    public PCMDataBlock read() {
        return thread.getBlock();
    }

    @Override
    public boolean isValid() {
        return thread != null;
    }

    @Override
    public String getContentType() {
        return backend.getContentType();
    }

    @Override
    public void close() throws IOException {
        thread.interrupt();
        thread = null;
        backend.close();
    }
}
