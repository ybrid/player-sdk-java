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

package io.ybrid.player.io;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.Session;
import io.ybrid.api.SubInfo;
import io.ybrid.api.metadata.Metadata;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class DataBlockMetadataUpdateThread extends Thread implements Consumer<DataBlock> {
    private static final int METADATA_BLOCK_QUEUE_SIZE = 32;

    private final @NotNull BlockingQueue<DataBlock> metadataBlockQueue = new LinkedBlockingQueue<>(METADATA_BLOCK_QUEUE_SIZE);
    private final @NotNull Session session;

    public DataBlockMetadataUpdateThread(String name, @NotNull Session session) {
        super(name);
        this.session = session;
    }

    @Override
    public void run() {
        Metadata metadata = null;
        Metadata oldMetadata = null;
        PlayoutInfo playoutInfo = null;
        PlayoutInfo oldPlayoutInfo = null;

        while (!isInterrupted()) {

            try {
                final DataBlock block = metadataBlockQueue.take();
                Metadata newMetadata = block.getMetadata();
                PlayoutInfo newPlayoutInfo = block.getPlayoutInfo();

                if (newMetadata != null && newMetadata != oldMetadata) {
                    metadata = newMetadata;
                    oldMetadata = newMetadata;
                }

                if (newPlayoutInfo != null && newPlayoutInfo != oldPlayoutInfo) {
                    playoutInfo = newPlayoutInfo;
                    oldPlayoutInfo = newPlayoutInfo;
                }

                if (metadata != null && !metadata.isValid()) {
                    try {
                        session.refresh(EnumSet.of(SubInfo.METADATA, SubInfo.PLAYOUT));
                        metadata = session.getMetadata();
                        playoutInfo = session.getPlayoutInfo();
                    } catch (IOException ignored) {
                    }
                }

                block.setMetadata(metadata);
                block.setPlayoutInfo(playoutInfo);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void accept(DataBlock dataBlock) {
        try {
            metadataBlockQueue.add(dataBlock);
        } catch (IllegalStateException e) {
            /* If the queue is full we fall behind. clear it and try again. */
            metadataBlockQueue.clear();
            metadataBlockQueue.add(dataBlock);
        }
    }
}
