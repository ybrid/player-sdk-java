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
import io.ybrid.player.io.*;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
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

    private final Session session;
    private MetadataConsumer metadataConsumer = null;
    private final DecoderFactory decoderFactory;
    private final AudioBackendFactory audioBackendFactory;
    private Decoder decoder;
    private AudioBackend audioBackend;
    private AudioBuffer audioSource;
    private PlaybackThread playbackThread;
    private PCMDataBlock initialAudioBlock;
    private MetadataThread metadataThread;
    private PlayerState playerState = PlayerState.STOPPED;

    private class PlaybackThread extends Thread {
        public static final double MAX_BUFFER_SLEEP = 0.3;

        private double bufferGoal;

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
            while (!isInterrupted() && audioSource.isValid() && audioSource.getBufferLength() < bufferGoal) {
                double diff = bufferGoal - audioSource.getBufferLength();
                if (diff > MAX_BUFFER_SLEEP) {
                    diff = MAX_BUFFER_SLEEP;
                } else if (diff < 0.) {
                    break;
                }

                try {
                    sleep((long) (diff*1000));
                } catch (InterruptedException e) {
                    break;
                }
            }
            playerStateChange(nextState);
        }

        @Override
        public void run() {
            Metadata oldMetadata = null;
            Metadata newMetadata;

            buffer(PlayerState.PLAYING);

            audioBackend.play();

            while (!isInterrupted()) {
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
                        } else if (!newMetadata.equals(oldMetadata)) {
                            metadataConsumer.onMetadataChange(newMetadata);
                        }
                    }

                    oldMetadata = newMetadata;
                }
            }

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
        private BlockingQueue<PCMDataBlock> metadataBlockQueue = new LinkedBlockingQueue<>(METADATA_BLOCK_QUEUE_SIZE);
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
    }

    @Override
    public void prepare() throws IOException {
        playerStateChange(PlayerState.PREPARING);

        playbackThread = new PlaybackThread("YbridPlayer Playback Thread", AUDIO_BUFFER_PREBUFFER); //NON-NLS
        metadataThread = new MetadataThread("YbridPlayer Metadata Thread", session); //NON-NLS

        decoder = decoderFactory.getDecoder(new BufferedByteDataSource(DataSourceFactory.getSourceBySession(session)));
        audioSource = new AudioBuffer(AUDIO_BUFFER_TARGET, decoder, metadataThread);

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
    public void stop() throws IOException {
        playbackThread.interrupt();
        metadataThread.interrupt();
    }

    @Override
    public Bouquet getBouquet() {
        return session.getBouquet();
    }

    @Override
    public void windToLive() throws IOException {
        session.windToLive();
    }

    @Override
    public void WindTo(Instant timestamp) throws IOException {
        session.WindTo(timestamp);
    }

    @Override
    public void Wind(long duration) throws IOException {
        session.Wind(duration);
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
    public void swapService(Service service) {
        session.swapService(service);
    }

    @Override
    public Metadata getMetadata() throws IOException {
        return session.getMetadata();
    }

    @Override
    public Service getCurrentService() {
        return session.getCurrentService();
    }

    @Override
    public void setMetadataConsumer(MetadataConsumer metadataConsumer) {
        if (metadataConsumer == null)
            metadataConsumer = NullMetadataConsumer.getInstance();
        this.metadataConsumer = metadataConsumer;
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}
