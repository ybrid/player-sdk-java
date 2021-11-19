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

package io.ybrid.player.player;

import io.ybrid.api.PlayoutInfo;
import io.ybrid.api.session.Session;
import io.ybrid.api.metadata.Sync;
import io.ybrid.api.session.Command;
import io.ybrid.api.transaction.CompletionState;
import io.ybrid.api.transaction.RequestExecutor;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.audio.BufferMuxer;
import io.ybrid.player.io.audio.BufferStatus;
import io.ybrid.player.io.audio.BufferStatusConsumer;
import io.ybrid.player.io.audio.PCMDataBlock;
import io.ybrid.player.io.audio.output.AudioOutput;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@ApiStatus.Internal
class PlaybackThread extends Thread {
    private static final double AUDIO_BUFFER_MAX_BEFORE_REBUFFER = 0.01; // [s]. Must be > 0.
    private static final double AUDIO_BUFFER_DEFAULT_GOAL = 10.0; // [s].

    private final @NotNull BlockingQueue<BufferStatus> bufferStateQueue = new LinkedBlockingQueue<>();
    private final @NotNull Set<Transaction> startTransactions = new HashSet<>();
    private final @NotNull Set<Transaction> stopTransactions = new HashSet<>();
    // We need to store this in a variable so add and remove gets the same one:
    private final @NotNull BufferStatusConsumer bufferStatusConsumer = bufferStateQueue::offer;
    private final @NotNull Session session;
    private final @NotNull BufferMuxer muxer;
    private final @NotNull AudioOutputFactory audioBackendFactory;
    private final @NotNull Consumer<@NotNull PlayerState> playerStateConsumer;
    private final @NotNull BiConsumer<@NotNull DataBlock, @Nullable PlayoutInfo> metadataConsumer;
    private final @NotNull RequestExecutor requestExecutor;
    private @Nullable AudioOutput audioOutput = null;
    private @Nullable PCMDataBlock initialAudioBlock = null;
    private double bufferGoal = AUDIO_BUFFER_DEFAULT_GOAL;
    private @Nullable Sync lastSentSync = null;
    private @Nullable PlayoutInfo lastSentPlayoutInfo = null;
    private @Nullable BufferStatus lastBufferStatus = null;

    public PlaybackThread(@NotNull @NonNls String name,
                          @NotNull Session session,
                          @NotNull BufferMuxer muxer,
                          @NotNull AudioOutputFactory audioBackendFactory,
                          @NotNull Consumer<@NotNull PlayerState> playerStateConsumer,
                          @NotNull BiConsumer<@NotNull DataBlock, @Nullable PlayoutInfo> metadataConsumer,
                          @NotNull RequestExecutor requestExecutor) {
        super(name);
        this.session = session;
        this.muxer = muxer;
        this.audioBackendFactory = audioBackendFactory;
        this.playerStateConsumer = playerStateConsumer;
        this.metadataConsumer = metadataConsumer;
        this.requestExecutor = requestExecutor;
    }

    private void setPlayerState(@NotNull PlayerState state) {
        playerStateConsumer.accept(state);
    }

    private void sendMetadata(@NotNull DataBlock block) {
        final @Nullable PlayoutInfo playoutInfoToForward;

        if (Objects.equals(lastSentSync, block.getSync()) && Objects.equals(lastSentPlayoutInfo, block.getPlayoutInfo()))
            return;
        lastSentSync = block.getSync();
        lastSentPlayoutInfo = block.getPlayoutInfo();

        if (lastSentPlayoutInfo != null && lastBufferStatus != null) {
            playoutInfoToForward = lastSentPlayoutInfo.adjustTimeToNextItem(Duration.ofMillis((long) (1000 * lastBufferStatus.getCurrent())));
        } else {
            playoutInfoToForward = lastSentPlayoutInfo;
        }
        metadataConsumer.accept(block, playoutInfoToForward);
    }

    public void prepare() throws IOException, InterruptedException {
        final @NotNull Transaction transaction;

        if (audioOutput != null)
            return;

        setPlayerState(PlayerState.PREPARING);
        transaction = requestExecutor.execute(Command.CONNECT_INITIAL_TRANSPORT.makeRequest());
        transaction.onAudioComplete(() -> {
            for (final @NotNull Transaction t : startTransactions) {
                t.setAudioComplete(CompletionState.DONE);
            }
        });
        transaction.waitControlComplete();
        transaction.assertSuccess();

        audioOutput = audioBackendFactory.getAudioOutput();
        initialAudioBlock = muxer.read();
        audioOutput.prepare(initialAudioBlock);
    }

    public void setBufferGoal(double bufferGoal) {
        this.bufferGoal = bufferGoal;
    }

    private void buffer() {
        setPlayerState(PlayerState.BUFFERING);
        try {
            while (!isInterrupted() && muxer.isValid()) {
                lastBufferStatus = bufferStateQueue.take();
                if (lastBufferStatus.getCurrent() > bufferGoal) {
                    break;
                }
            }
        } catch (InterruptedException ignored) {
        }
        setPlayerState(PlayerState.PLAYING);
    }

    @Override
    public void run() {
        @NotNull PCMDataBlock block;

        try {
            prepare();
        } catch (Throwable e) {
            setPlayerState(PlayerState.ERROR);
            return;
        }
        assert audioOutput != null;
        block = Objects.requireNonNull(initialAudioBlock);

        muxer.addBufferStatusConsumer(bufferStatusConsumer);
        buffer();
        audioOutput.play();
        while (!isInterrupted()) {
            try {
                audioOutput.write(block);
            } catch (IOException e) {
                setPlayerState(PlayerState.ERROR);
                break;
            }

            sendMetadata(block);

            while (!bufferStateQueue.isEmpty())
                lastBufferStatus = bufferStateQueue.poll();

            if (lastBufferStatus != null && lastBufferStatus.getCurrent() < AUDIO_BUFFER_MAX_BEFORE_REBUFFER && !muxer.isInHandover()) {
                buffer();
            }

            try {
                block = muxer.read();
            } catch (IOException e) {
                setPlayerState(PlayerState.ERROR);
                break;
            }
        }
        muxer.removeBufferStatusConsumer(bufferStatusConsumer);

        try {
            audioOutput.close();
        } catch (IOException ignored) {
        }
        audioOutput = null;
        for (final @NotNull Transaction t : stopTransactions) {
            t.setAudioComplete(CompletionState.DONE);
        }

        setPlayerState(PlayerState.STOPPED);
    }

    public void stop(@Nullable Transaction transaction) {
        if (transaction != null)
            stopTransactions.add(transaction);
        interrupt();
    }

    public void start(@Nullable Transaction transaction) {
        if (transaction != null)
            startTransactions.add(transaction);
        super.start();
    }
}
