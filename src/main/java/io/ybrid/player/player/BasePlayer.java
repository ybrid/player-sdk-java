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
import io.ybrid.api.util.QualityMap.MediaTypeMap;
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
import java.util.logging.Logger;

public class BasePlayer extends PlayerStub {
    static final @NonNls Logger LOGGER = Logger.getLogger(BasePlayer.class.getName());

    private final @NotNull Control control;
    protected final @NotNull PlaybackThread playbackThread;

    private @NotNull Control buildPlayerControl() {
        return new Control() {
            private void stop(@Nullable Transaction transaction) {
                playbackThread.stop(transaction);
                try {
                    muxer.close();
                } catch (IOException ignored) {
                }
                session.detachPlayer(control);
            }

            @Override
            public void onDetach(@NotNull Session unused) {
                stop(null);
            }

            @Override
            public @NotNull MediaTypeMap getAcceptedMediaTypes() {
                return decoderFactory.getSupportedMediaTypes();
            }

            @Override
            public void connectTransport(@NotNull ServiceTransportDescription transportDescription) throws Exception {
                final @Nullable Decoder decoder;

                /*
                 * We disconnect the inputEOFCallback while we connect a new source to avoid race conditions between the
                 * server sending EOF and us adding the new buffer.
                 */
                muxer.setInputEOFCallback(null);
                decoder = decoderFactory.getDecoder(new BufferedByteDataSource(dataSourceFactory.getSource(transportDescription)));
                if (decoder == null) {
                    LOGGER.warning("Can not create decoder for new input.");
                } else {
                    muxer.addBuffer(decoder, transportDescription);
                }
                muxer.setInputEOFCallback(() -> onInputEOF());
            }

            @Override
            public <C extends io.ybrid.api.player.Command<C>> void executeTransaction(@NotNull RequestBasedTransaction<Request<C>> transaction) throws Throwable {
                final @NotNull Request<C> request = transaction.getRequest();
                final @NotNull C command = request.getCommand();

                LOGGER.info("Trying: " + request + " with " + command);

                if (command.equals(SimpleCommand.PREPARE)) {
                    playbackThread.prepare();
                } else if (command.equals(SimpleCommand.PLAY)) {
                    playbackThread.start(transaction);
                } else if (command.equals(SimpleCommand.STOP)) {
                    stop(transaction);
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
            final @NotNull Transaction transaction = execute(Command.RECONNECT_TRANSPORT.makeRequest());
            try {
                transaction.waitControlComplete();
            } catch (InterruptedException ignored) {
            }
            if (transaction.getError() != null)
                LOGGER.warning("Connecting new source failed: " + transaction.getError()); //NON-NLS
        }

        try {
            final @NotNull Transaction transaction = execute(Command.REFRESH.makeRequest(EnumSet.of(SubInfo.VALIDITY)));
            transaction.waitControlComplete();
            transaction.assertSuccess();
        } catch (Throwable e) {
            LOGGER.warning("Validating session failed.");
        }
    }

    protected void onPlayerStateChange(@NotNull PlayerState state) {
        // TODO.
    }

    protected void onMetadataChange(@NotNull DataBlock block, @Nullable PlayoutInfo playoutInfo) {
        // no-op.
    }

    public BasePlayer(@NotNull Session session, @Nullable DecoderFactory externalDecoderFactory, @NotNull AudioOutputFactory audioOutputFactory, @Nullable DataSourceFactory dataSourceFactory) {
        super(session, externalDecoderFactory, audioOutputFactory, dataSourceFactory);
        this.playbackThread = new PlaybackThread("YbridPlayer Playback Thread", session, muxer, audioOutputFactory, this::onPlayerStateChange, this::onMetadataChange, this);
        this.control = buildPlayerControl();
        session.attachPlayer(this.control);
    }
}
