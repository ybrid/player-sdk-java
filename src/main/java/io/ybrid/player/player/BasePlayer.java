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
import io.ybrid.api.Session;
import io.ybrid.api.SubInfo;
import io.ybrid.api.player.Control;
import io.ybrid.api.player.SimpleCommand;
import io.ybrid.api.session.Command;
import io.ybrid.api.transaction.Request;
import io.ybrid.api.transaction.RequestBasedTransaction;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.api.transport.ServiceTransportDescription;
import io.ybrid.player.io.BufferedByteDataSource;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.DataSourceFactory;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import io.ybrid.player.io.decoder.Decoder;
import io.ybrid.player.io.decoder.DecoderFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Map;
import java.util.logging.Logger;

public class BasePlayer extends PlayerStub {
    static final @NonNls Logger LOGGER = Logger.getLogger(BasePlayer.class.getName());

    private final @NotNull Control control;
    protected final @NotNull PlaybackThread playbackThread;

    private @NotNull Control buildPlayerControl() {
        return new Control() {
            @Override
            public void onDetach(@NotNull Session unused) {
                try {
                    close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public @NotNull Map<String, Double> getAcceptedMediaFormats() {
                return decoderFactory.getSupportedFormats();
            }

            @Override
            public void connectTransport(@NotNull ServiceTransportDescription transportDescription) throws Exception {
                final @Nullable Decoder decoder;

                /*
                 * We disconnect the inputEOFCallback while we connect a new source to avoid race conditions between the
                 * server sending EOF and us adding the new buffer.
                 */
                muxer.setInputEOFCallback(null);
                decoder = decoderFactory.getDecoder(new BufferedByteDataSource(DataSourceFactory.getSourceByTransportDescription(transportDescription)));
                if (decoder == null) {
                    LOGGER.warning("Can not create decoder for new input.");
                } else {
                    muxer.addBuffer(decoder, transportDescription);
                }
                muxer.setInputEOFCallback(() -> onInputEOF());
            }

            @Override
            public <C extends io.ybrid.api.player.Command<C>> void executeTransaction(@NotNull RequestBasedTransaction<Request<C>> transaction) throws Exception {
                final @NotNull Request<C> request = transaction.getRequest();
                final @NotNull C command = request.getCommand();

                LOGGER.info("Trying: " + request + " with " + command);

                if (command.equals(SimpleCommand.PREPARE)) {
                    playbackThread.prepare();
                } else if (command.equals(SimpleCommand.PLAY)) {
                    playbackThread.start();
                } else if (command.equals(SimpleCommand.STOP)) {
                    playbackThread.interrupt();
                    try {
                        muxer.close();
                    } catch (IOException ignored) {
                    }
                    session.detachPlayer(control);
                } else {
                    throw new UnsupportedOperationException("Unknown request " + request + " with command " + command);
                }
            }
        };
    }

    private void onInputEOF() {
        LOGGER.info("Input EOF reached");

        if (!autoReconnect)
            return;

        if (!session.isValid())
            return;

        if (muxer.isInHandover())
            return;

        LOGGER.info("Auto Reconnect is enabled, we have a valid session, and are not in a handover. Connecting new source and validating session.");

        {
            final @NotNull Transaction transaction = session.createTransaction(Command.RECONNECT_TRANSPORT.makeRequest());
            transaction.run();
            if (transaction.getError() != null)
                LOGGER.warning("Connecting new source failed: " + transaction.getError()); //NON-NLS
        }

        try {
            executeRequestAsTransaction(Command.REFRESH, EnumSet.of(SubInfo.VALIDITY));
        } catch (IOException e) {
            LOGGER.warning("Validating session failed.");
        }
    }

    protected void onPlayerStateChange(@NotNull PlayerState state) {
        // TODO.
    }

    protected void onMetadataChange(@NotNull DataBlock block, @Nullable PlayoutInfo playoutInfo) {
        // no-op.
    }

    public BasePlayer(@NotNull Session session, @Nullable DecoderFactory externalDecoderFactory, @NotNull AudioOutputFactory externalAudioBackendFactory) {
        super(session, externalDecoderFactory, externalAudioBackendFactory);
        this.playbackThread = new PlaybackThread("YbridPlayer Playback Thread", session, muxer, externalAudioBackendFactory, this::onPlayerStateChange, this::onMetadataChange);
        this.control = buildPlayerControl();
        session.attachPlayer(this.control);
    }

    @Override
    public void prepare() throws IOException {
        executeRequestAsTransaction(SimpleCommand.PREPARE);
    }

    @Override
    public void play() throws IOException {
        executeRequestAsTransaction(SimpleCommand.PLAY);
    }

    @Override
    public void stop() throws IOException {
        executeRequestAsTransaction(SimpleCommand.STOP);
    }
}
