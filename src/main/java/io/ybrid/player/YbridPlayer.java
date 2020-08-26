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
import io.ybrid.api.bouquet.Bouquet;
import io.ybrid.api.bouquet.Service;
import io.ybrid.api.metadata.ItemType;
import io.ybrid.api.metadata.Metadata;
import io.ybrid.player.io.BufferedByteDataSource;
import io.ybrid.player.io.DataSourceFactory;
import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.audio.BufferMuxer;
import io.ybrid.player.io.audio.BufferStatus;
import io.ybrid.player.io.audio.BufferStatusConsumer;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * This implements a Ybrid capable {@link Player}.
 *
 * See also {@link SessionClient}.
 */
public class YbridPlayer implements Player {
    static final @NonNls Logger LOGGER = Logger.getLogger(YbridPlayer.class.getName());

    private static final double AUDIO_BUFFER_TARGET = 10; /* [s] */
    private static final double AUDIO_BUFFER_PREBUFFER = 1.5; /* [s] */

    private final Session session;
    private MetadataConsumer metadataConsumer = null;
    private final DecoderFactory decoderFactory;
    private final AudioBackendFactory audioBackendFactory;
    private AudioBackend audioBackend;
    private final @NotNull BufferMuxer muxer;
    private PlaybackThread playbackThread;
    private PCMDataBlock initialAudioBlock;
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

        private @Nullable BufferStatus buffer(PlayerState nextState) {
            BufferStatus ret = null;

            playerStateChange(PlayerState.BUFFERING);
            try {
                while (!isInterrupted() && muxer.isValid()) {
                    ret = bufferStateQueue.take();
                    if (ret.getCurrent() > bufferGoal) {
                        break;
                    }
                }
            } catch (InterruptedException ignored) {
            }
            playerStateChange(nextState);

            return ret;
        }

        @Override
        public void run() {
            Metadata oldMetadata = session.getMetadata();
            PlayoutInfo oldPlayoutInfo = null;
            PlayoutInfo forwardedPlayoutInfo = session.getPlayoutInfo();
            BufferStatus lastStatus;

            muxer.addBufferStatusConsumer(bufferStatusConsumer);
            lastStatus = buffer(PlayerState.PLAYING);

            audioBackend.play();

            while (!isInterrupted()) {
                final Metadata newMetadata;
                final PlayoutInfo newPlayoutInfo;
                boolean blockUpdatesMetadata = false;
                PCMDataBlock block = initialAudioBlock;
                final BufferStatus status;

                try {
                    if (block == null) {
                        block = muxer.read();
                    } else {
                        initialAudioBlock = null;
                    }
                    audioBackend.write(block);
                } catch (IOException e) {
                    playerStateChange(PlayerState.ERROR);
                    break;
                }

                newMetadata = block.getMetadata();
                newPlayoutInfo = block.getPlayoutInfo();

                if (newMetadata != null && newMetadata.isValid() && !Objects.equals(oldMetadata, newMetadata)) {
                    blockUpdatesMetadata = true;
                    oldMetadata = newMetadata;
                }

                if (newPlayoutInfo != null && !Objects.equals(oldPlayoutInfo, newPlayoutInfo)) {
                    blockUpdatesMetadata = true;
                    oldPlayoutInfo = newPlayoutInfo;
                    if (lastStatus == null) {
                        forwardedPlayoutInfo = newPlayoutInfo;
                    } else {
                        forwardedPlayoutInfo = newPlayoutInfo.adjustTimeToNextItem(Duration.ofMillis((long) (1000*lastStatus.getCurrent())));
                    }
                }

                if (blockUpdatesMetadata)
                    distributeMetadata(oldMetadata, forwardedPlayoutInfo);

                /* empty queue but for the last entry. */
                while (bufferStateQueue.size() > 1)
                    bufferStateQueue.poll();

                status = bufferStateQueue.poll();
                if (status != null) {
                    lastStatus = status;
                    if (status.getCurrent() < AUDIO_BUFFER_MAX_BEFORE_REBUFFER && !muxer.isInHandover()) {
                        lastStatus = buffer(playerState);
                    }
                }
            }

            muxer.removeBufferStatusConsumer(bufferStatusConsumer);

            close(audioBackend);
            audioBackend = null;
            close(muxer);

            playerStateChange(PlayerState.STOPPED);
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
        this.muxer = new BufferMuxer(session);
        this.session = session;
        this.decoderFactory = decoderFactory;
        this.audioBackendFactory = audioBackendFactory;
        setMetadataConsumer(null);
    }

    private void assertPrepared() throws IOException {
        if (audioBackend == null)
            prepare();
    }

    private void distributeMetadata(@NotNull Metadata metadata, @NotNull PlayoutInfo playoutInfo) {
        metadataConsumer.onMetadataChange(metadata);
        metadataConsumer.onPlayoutInfoChange(playoutInfo);
        capabilitiesChange();
        bouquetChange();
    }

    @SuppressWarnings("ParameterHidesMemberVariable")
    private void playerStateChange(PlayerState playerState) {
        if (this.playerState == PlayerState.ERROR)
            return;
        LOGGER.info("playerState: " + this.playerState + " -> " + playerState);
        this.playerState = playerState;
        metadataConsumer.onPlayerStateChange(playerState);
        capabilitiesChange();
        bouquetChange();
    }

    private void capabilitiesChange() {
        if (session.hasChanged(SubInfo.CAPABILITIES))
            metadataConsumer.onCapabilitiesChange(session.getCapabilities().makePlayerSet());
    }

    private void bouquetChange() {
        if (session.hasChanged(SubInfo.BOUQUET))
            metadataConsumer.onBouquetChange(session.getBouquet());
    }

    private void connectSource() throws IOException {
        session.setAcceptedMediaFormats(decoderFactory.getSupportedFormats());

        muxer.addBuffer(decoderFactory.getDecoder(new BufferedByteDataSource(DataSourceFactory.getSourceBySession(session))));
    }

    @Override
    public void prepare() throws IOException {
        playerStateChange(PlayerState.PREPARING);

        playbackThread = new PlaybackThread("YbridPlayer Playback Thread", AUDIO_BUFFER_PREBUFFER); //NON-NLS

        connectSource();

        audioBackend = audioBackendFactory.getAudioBackend();
        initialAudioBlock = muxer.read();
        audioBackend.prepare(initialAudioBlock);
    }

    @Override
    public void play() throws IOException {
        assertPrepared();
        playbackThread.start();
    }

    @Override
    public void stop() {
        playbackThread.interrupt();
    }


    @Override
    public io.ybrid.api.@NotNull CapabilitySet getCapabilities() {
        return session.getCapabilities().makePlayerSet();
    }

    @Override
    public @NotNull Bouquet getBouquet() {
        return session.getBouquet();
    }

    @Override
    public void refresh(@NotNull SubInfo what) throws IOException {
        session.refresh(what);
        capabilitiesChange();
        bouquetChange();
    }

    @Override
    public void refresh(@NotNull EnumSet<SubInfo> what) throws IOException {
        session.refresh(what);
        capabilitiesChange();
        bouquetChange();
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
    public boolean hasChanged(@NotNull SubInfo what) {
        return session.hasChanged(what);
    }

    @Override
    public @NotNull Metadata getMetadata() {
        return session.getMetadata();
    }

    @Override
    public @NotNull PlayoutInfo getPlayoutInfo() {
        return session.getPlayoutInfo();
    }

    @Override
    public @NotNull Service getCurrentService() {
        return session.getCurrentService();
    }

    @Override
    public boolean isValid() {
        return session.isValid();
    }

    @Override
    public void setMetadataConsumer(MetadataConsumer metadataConsumer) {
        if (metadataConsumer == null)
            metadataConsumer = NullMetadataConsumer.getInstance();
        this.metadataConsumer = metadataConsumer;
    }

    @Override
    public void addBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        muxer.addBufferStatusConsumer(consumer);
    }

    @Override
    public void removeBufferStatusConsumer(@NotNull BufferStatusConsumer consumer) {
        muxer.removeBufferStatusConsumer(consumer);
    }

    @Override
    public void close() {
        stop();
    }
}
