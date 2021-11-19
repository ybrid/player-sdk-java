/*
 * Copyright (c) 2021 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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
import io.ybrid.api.SubInfo;
import io.ybrid.api.bouquet.Bouquet;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.DataSourceFactory;
import io.ybrid.player.io.audio.BufferStatusProvider;
import io.ybrid.player.io.audio.output.AudioOutput;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import io.ybrid.player.io.decoder.Decoder;
import io.ybrid.player.io.decoder.DecoderFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * This implements a Ybrid capable {@link Player}.
 *
 * See also {@link MediaController}.
 */
public class YbridPlayer extends BasePlayer implements MediaController, BufferStatusProvider {
    static final @NonNls Logger LOGGER = Logger.getLogger(YbridPlayer.class.getName());

    private static final double AUDIO_BUFFER_PREBUFFER = 1.5; /* [s] */

    private PlayerState playerState = PlayerState.STOPPED;

    /**
     * Creates a new instance of the player using {@link AudioOutputFactory#getDefaultFactory()} as {@link AudioOutputFactory}.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     */
    @ApiStatus.Experimental
    public YbridPlayer(@NotNull Session session) {
        this(session, null, AudioOutputFactory.getDefaultFactory(), null);
    }

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param audioOutputFactory The {@link AudioOutputFactory} used to create a {@link AudioOutput} to interact with the host audio output.
     */
    public YbridPlayer(@NotNull Session session, @NotNull AudioOutputFactory audioOutputFactory) {
        this(session, null, audioOutputFactory, null);
    }

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param externalDecoderFactory The {@link DecoderFactory} used to create a {@link Decoder} for the stream.
     * @param audioOutputFactory The {@link AudioOutputFactory} used to create a {@link AudioOutput} to interact with the host audio output.
     */
    public YbridPlayer(@NotNull Session session, @Nullable DecoderFactory externalDecoderFactory, @NotNull AudioOutputFactory audioOutputFactory) {
        this(session, externalDecoderFactory, audioOutputFactory, null);
    }

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param externalDecoderFactory The {@link DecoderFactory} used to create a {@link Decoder} for the stream.
     * @param audioOutputFactory The {@link AudioOutputFactory} used to create a {@link AudioOutput} to interact with the host audio output.
     * @param dataSourceFactory The {@link DataSourceFactory} used to connect to the services.
     */
    public YbridPlayer(@NotNull Session session, @Nullable DecoderFactory externalDecoderFactory, @NotNull AudioOutputFactory audioOutputFactory, @Nullable DataSourceFactory dataSourceFactory) {
        super(session, externalDecoderFactory, audioOutputFactory, dataSourceFactory);
        this.playbackThread.setBufferGoal(AUDIO_BUFFER_PREBUFFER);
    }

    @Override
    protected void onMetadataChange(@NotNull DataBlock block, @Nullable PlayoutInfo playoutInfo) {
        super.onMetadataChange(block, playoutInfo);

        metadataConsumer.onMetadataChange(session.getMetadataMixer().resolveMetadata(block.getSync()));

        if (playoutInfo != null)
            metadataConsumer.onPlayoutInfoChange(playoutInfo);
        capabilitiesChange();
        bouquetChange();
    }

    @Override
    protected void onPlayerStateChange(@NotNull PlayerState state) {
        super.onPlayerStateChange(state);

        if (this.playerState == PlayerState.ERROR)
            return;
        LOGGER.info("playerState: " + this.playerState + " -> " + state);
        this.playerState = state;
        metadataConsumer.onPlayerStateChange(state);
        capabilitiesChange();
        bouquetChange();
    }

    private void capabilitiesChange() {
        if (session.hasChanged(SubInfo.CAPABILITIES))
            metadataConsumer.onCapabilitiesChange(session.getCapabilities().makePlayerSet());
    }

    private void bouquetChange() {
        if (session.hasChanged(SubInfo.BOUQUET))
            metadataConsumer.onBouquetChange(session.getMetadataMixer().getBouquet());
    }

    @Override
    public io.ybrid.api.@NotNull CapabilitySet getCapabilities() {
        return session.getCapabilities().makePlayerSet();
    }

    @Override
    public @NotNull Bouquet getBouquet() {
        return session.getMetadataMixer().getBouquet();
    }

    @Override
    public boolean hasChanged(@NotNull SubInfo what) {
        return session.hasChanged(what);
    }

    @Override
    public @NotNull PlayoutInfo getPlayoutInfo() {
        return session.getPlayoutInfo();
    }

    @Override
    public boolean isValid() {
        return session.isValid();
    }
}
