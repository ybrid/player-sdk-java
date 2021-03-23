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
import io.ybrid.api.metadata.MetadataMixer;
import io.ybrid.api.metadata.Sync;
import io.ybrid.api.session.Command;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.player.transaction.RequestExecutor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class DataBlockMetadataUpdateThread extends Thread implements Consumer<DataBlock> {
    private static final int METADATA_BLOCK_QUEUE_SIZE = 32;

    private final @NotNull BlockingQueue<DataBlock> metadataBlockQueue = new LinkedBlockingQueue<>(METADATA_BLOCK_QUEUE_SIZE);
    private final @NotNull Session session;
    private final @NotNull RequestExecutor requestExecutor;

    public DataBlockMetadataUpdateThread(@NonNls String name, @NotNull Session session, @NotNull RequestExecutor requestExecutor) {
        super(name);
        this.session = session;
        this.requestExecutor = requestExecutor;
    }

    @Override
    public void run() {
        final @NotNull MetadataMixer metadataMixer = session.getMetadataMixer();
        Sync oldSync = null;
        PlayoutInfo playoutInfo = null;
        PlayoutInfo oldPlayoutInfo = null;

        while (!isInterrupted()) {

            try {
                final DataBlock block = metadataBlockQueue.take();
                final @NotNull Sync newSync = block.getSync();
                final @Nullable PlayoutInfo newPlayoutInfo = block.getPlayoutInfo();
                boolean playoutInfoChanged = false;

                if (newPlayoutInfo != null && newPlayoutInfo != oldPlayoutInfo) {
                    playoutInfo = newPlayoutInfo;
                    oldPlayoutInfo = newPlayoutInfo;
                    playoutInfoChanged = true;
                }

                if (playoutInfoChanged || !newSync.equals(oldSync)) {
                    final @NotNull Transaction transaction = requestExecutor.executeTransaction(Command.REFRESH.makeRequest(newSync));
                    transaction.waitControlComplete();
                    if (transaction.getError() == null) {
                        playoutInfo = session.getPlayoutInfo();
                    }
                }

                block.setPlayoutInfo(playoutInfo);

                oldSync = newSync;
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
