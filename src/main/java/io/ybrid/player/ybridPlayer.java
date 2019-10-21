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

package io.ybrid.player;

import io.ybrid.api.*;
import io.ybrid.player.io.*;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;

/**
 * This implements a ybrid capable {@link Player}.
 *
 * See also {@link SessionClient}.
 */
public class ybridPlayer implements Player {
    private static final double AUDIO_BUFFER_TARGET = 10; /* [s] */

    private Session session;
    private MetadataConsumer metadataConsumer = null;
    private DecoderFactory decoderFactory;
    private AudioBackendFactory audioBackendFactory;
    private Decoder decoder;
    private AudioBackend audioBackend;
    private PCMDataSource audioSource;
    private PlaybackThread thread;
    private PCMDataBlock initialAudioBlock;

    private class PlaybackThread extends Thread {
        public PlaybackThread(String name) {
            super(name);
        }

        private void close(Closeable obj) {
            try {
                obj.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public void run() {
            Metadata oldMetadata = null;
            Metadata newMetadata;

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
                    break;
                }

                newMetadata = block.getMetadata();

                if (newMetadata != null) {
                    if (!newMetadata.isValid()) {
                        try {
                            newMetadata = session.getMetadata();
                        } catch (IOException ignored) {
                        }
                    }
                    if (oldMetadata == null) {
                        metadataConsumer.onMetadataChange(newMetadata);
                    } else if (!newMetadata.equals(oldMetadata)) {
                        metadataConsumer.onMetadataChange(newMetadata);
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
        }
    }

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param decoderFactory The {@link DecoderFactory} used to create a {@link Decoder} for the stream.
     * @param audioBackendFactory The {@link AudioBackendFactory} used to create a {@link AudioBackend} to interact with the host audio output.
     */
    public ybridPlayer(Session session, DecoderFactory decoderFactory, AudioBackendFactory audioBackendFactory) {
        this.session = session;
        this.decoderFactory = decoderFactory;
        this.audioBackendFactory = audioBackendFactory;
    }

    private void assertPrepared() throws IOException {
        if (audioBackend == null)
            prepare();
    }

    @Override
    public void prepare() throws IOException {
        DataSource streamSource = new BufferedByteDataSource(DataSourceFactory.getSourceBySession(session));
        decoder = decoderFactory.getDecoder(streamSource);
        audioSource = decoder;

        audioBackend = audioBackendFactory.getAudioBackend();
        initialAudioBlock = audioSource.read();
        audioBackend.prepare(initialAudioBlock);

        thread = new PlaybackThread("ybridPlayer Playback Thread");
    }

    @Override
    public void play() throws IOException {
        assertPrepared();
        thread.start();
    }

    @Override
    public void stop() throws IOException {
        thread.interrupt();
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
        this.metadataConsumer = metadataConsumer;
    }

    @Override
    public void close() throws IOException {
        stop();
    }
}