/*
 * Copyright (c) 2019 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player;

import io.ybrid.api.*;
import io.ybrid.player.io.BufferedByteDataSource;
import io.ybrid.player.io.DataSourceFactory;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.audio.Buffer;
import io.ybrid.player.io.audio.BufferStatus;
import io.ybrid.player.io.audio.BufferStatusConsumer;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

/**
 * This implements a Ybrid capable {@link Player}.
 *
 * See also {@link SessionClient}.
 */
public class YbridPlayer implements Player {
    private static final double AUDIO_BUFFER_TARGET = 10; /* [s] */
    private static final double AUDIO_BUFFER_PREBUFFER = 1.5; /* [s] */
    private static final int METADATA_BLOCK_QUEUE_SIZE = 32;

    // Proxy list, used to store BufferStatusConsumers when in non-prepared state.
    final List<BufferStatusConsumer> bufferStatusConsumers = new ArrayList<>();
    private final Session session;
    private MetadataConsumer metadataConsumer = null;
    private final DecoderFactory decoderFactory;
    private final AudioBackendFactory audioBackendFactory;
    private Decoder decoder;
    private AudioBackend audioBackend;
    private Buffer audioSource;
    private PlaybackThread playbackThread;
    private PCMDataBlock initialAudioBlock;
    private MetadataThread metadataThread;
    private PlayerState playerState = PlayerState.STOPPED;

    private class PlaybackThread extends Thread {
        private static final double AUDIO_BUFFER_MAX_BEFORE_REBUFFER = 0.01; // [s]. Must be > 0.

        final BlockingQueue<BufferStatus> bufferStateQueue = new LinkedBlockingQueue<>();
        // We need to store this in a variable so add and remove gets the same one:
        final BufferStatusConsumer bufferStatusConsumer = bufferStateQueue::offer;
        private final double bufferGoal;

        public PlaybackThread(String name, double bufferGoal) {
            super(name);
            this.bufferGoal = bufferGoal;
        }

        private void close(Closeable obj) {
            try {
                obj.close();
            } catch (IOException ignored) {
            }
        }

        private void buffer(PlayerState nextState) {
            playerStateChange(PlayerState.BUFFERING);
            try {
                while (!isInterrupted() && audioSource.isValid()) {
                    if (bufferStateQueue.take().getCurrent() > bufferGoal) {
                        break;
                    }
                }
            } catch (InterruptedException ignored) {
            }
            playerStateChange(nextState);
        }

        @Override
        public void run() {
            Metadata oldMetadata = null;
            Metadata newMetadata;

            audioSource.addBufferStatusConsumer(bufferStatusConsumer);
            buffer(PlayerState.PLAYING);

            audioBackend.play();

            while (!isInterrupted()) {
                final BufferStatus status;
                PCMDataBlock block = initialAudioBlock;

                try {
                    if (block == null) {
                        block = audioSource.read();
                    } else {
                        initialAudioBlock = null;
                    }
                    audioBackend.write(block);
                } catch (IOException e) {
                    playerStateChange(PlayerState.ERROR);
                    break;
                }

                newMetadata = block.getMetadata();

                if (newMetadata != null) {
                    if (newMetadata.isValid()) {
                        if (oldMetadata == null) {
                            metadataConsumer.onMetadataChange(newMetadata);
                            capabilitiesChange();
                        } else if (!newMetadata.equals(oldMetadata)) {
                            metadataConsumer.onMetadataChange(newMetadata);
                            capabilitiesChange();
                        }
                    }

                    oldMetadata = newMetadata;
                }

                /* empty queue but for the last entry. */
                while (bufferStateQueue.size() > 1)
                    bufferStateQueue.poll();

                status = bufferStateQueue.poll();
                if (status != null) {
                    if (status.getCurrent() < AUDIO_BUFFER_MAX_BEFORE_REBUFFER) {
                        buffer(playerState);
                    }
                }
            }

            audioSource.removeBufferStatusConsumer(bufferStatusConsumer);

            close(audioBackend);
            audioBackend = null;
            close(audioSource);
            audioSource = null;
            close(decoder);
            decoder = null;

            playerStateChange(PlayerState.STOPPED);
        }
    }

    private static class MetadataThread extends Thread implements Consumer<PCMDataBlock> {
        private final BlockingQueue<PCMDataBlock> metadataBlockQueue = new LinkedBlockingQueue<>(METADATA_BLOCK_QUEUE_SIZE);
        private final Session session;

        MetadataThread(String name, Session session) {
            super(name);
            this.session = session;
        }

        @Override
        public void run() {
            Metadata metadata = null;
            Metadata oldMetadata = null;

            while (!isInterrupted()) {

                try {
                    final PCMDataBlock block = metadataBlockQueue.take();
                    Metadata newMetadata = block.getMetadata();

                    if (newMetadata != null && newMetadata != oldMetadata) {
                        metadata = newMetadata;
                        oldMetadata = newMetadata;
                    }

                    if (metadata != null && !metadata.isValid()) {
                        try {
                            metadata = session.getMetadata();
                        } catch (IOException ignored) {
                        }
                    }

                    block.setMetadata(metadata);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        @Override
        public void accept(PCMDataBlock pcmDataBlock) {
            try {
                metadataBlockQueue.add(pcmDataBlock);
            } catch (IllegalStateException e) {
                /* If the queue is full we fall behind. clear it and try again. */
                metadataBlockQueue.clear();
                metadataBlockQueue.add(pcmDataBlock);
            }
        }
    }

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param decoderFactory The {@link DecoderFactory} used to create a {@link Decoder} for the stream.
     * @param audioBackendFactory The {@link AudioBackendFactory} used to create a {@link AudioBackend} to interact with the host audio output.
     */
    public YbridPlayer(Session session, DecoderFactory decoderFactory, AudioBackendFactory audioBackendFactory) {
        this.session = session;
        this.decoderFactory = decoderFactory;
        this.audioBackendFactory = audioBackendFactory;
        setMetadataConsumer(null);
    }

    private void assertPrepared() throws IOException {
        if (audioBackend == null)
            prepare();
    }

    @SuppressWarnings("ParameterHidesMemberVariable")
    private void playerStateChange(PlayerState playerState) {
        if (this.playerState == PlayerState.ERROR)
            return;
        this.playerState = playerState;
        metadataConsumer.onPlayerStateChange(playerState);
        capabilitiesChange();
    }

    private void capabilitiesChange() {
        if (session.haveCapabilitiesChanged())
            metadataConsumer.onCapabilitiesChange(session.getCapabilities().makePlayerSet());
    }

    @Override
    public void prepare() throws IOException {
        playerStateChange(PlayerState.PREPARING);

        playbackThread = new PlaybackThread("YbridPlayer Playback Thread", AUDIO_BUFFER_PREBUFFER); //NON-NLS
        metadataThread = new MetadataThread("YbridPlayer Metadata Thread", session); //NON-NLS

        session.setAcceptedMediaFormats(decoderFactory.getSupportedFormats());
        decoder = decoderFactory.getDecoder(new BufferedByteDataSource(DataSourceFactory.getSourceBySession(session)));
        audioSource = new Buffer(AUDIO_BUFFER_TARGET, decoder, metadataThread);

        for (BufferStatusConsumer consumer : bufferStatusConsumers)
            audioSource.addBufferStatusConsumer(consumer);

        audioBackend = audioBackendFactory.getAudioBackend();
        initialAudioBlock = audioSource.read();
        audioBackend.prepare(initialAudioBlock);
    }

    @Override
    public void play() throws IOException {
        assertPrepared();
        playbackThread.start();
        metadataThread.start();
    }

    @Override
    public void stop() {
        playbackThread.interrupt();
        metadataThread.interrupt();
    }


    @Override
    public io.ybrid.api.@NotNull CapabilitySet getCapabilities() {
        return session.getCapabilities().makePlayerSet();
    }

    @Override
    public boolean haveCapabilitiesChanged() {
        return session.haveCapabilitiesChanged();
    }


    @Override
    public @NotNull Bouquet getBouquet() throws IOException {
        return session.getBouquet();
    }

    @Override
    public void windToLive() throws IOException {
        session.windToLive();
    }

    @Override
    public void windTo(@NotNull Instant timestamp) throws IOException {
        session.windTo(timestamp);
    }

    @Override
    public void wind(@NotNull Duration duration) throws IOException {
        session.wind(duration);
    }

    @Override
    public void skipForwards(ItemType itemType) throws IOException {
        session.skipForwards(itemType);
    }

    @Override
    public void skipBackwards(ItemType itemType) throws IOException {
        session.skipBackwards(itemType);
    }

    @Override
    public void swapItem(SwapMode mode) throws IOException {
        session.swapItem(mode);
    }

    @Override
    public void swapService(@NotNull Service service) throws IOException {
        session.swapService(service);
    }

    @Override
    public void swapToMain() throws IOException {
        session.swapToMain();
    }

    @Override
    public @NotNull Metadata getMetadata() throws IOException {
        return session.getMetadata();
    }

    @Override
    public @NotNull PlayoutInfo getPlayoutInfo() throws IOException {
        return session.getPlayoutInfo();
    }

    @Override
    public @NotNull Service getCurrentService() throws IOException {
        return session.getCurrentService();
    }

    @Override
    public void setMetadataConsumer(MetadataConsumer metadataConsumer) {
        if (metadataConsumer == null)
            metadataConsumer = NullMetadataConsumer.getInstance();
        this.metadataConsumer = metadataConsumer;
    }

    @Override
    public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        if (!bufferStatusConsumers.contains(consumer))
            bufferStatusConsumers.add(consumer);

        if (audioSource != null)
            audioSource.addBufferStatusConsumer(consumer);
    }

    @Override
    public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        bufferStatusConsumers.remove(consumer);

        if (audioSource != null)
            audioSource.removeBufferStatusConsumer(consumer);
    }

    @Override
    public void close() {
        stop();
    }
}
