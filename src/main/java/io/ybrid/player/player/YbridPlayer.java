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
import io.ybrid.api.Session;
import io.ybrid.api.SubInfo;
import io.ybrid.api.SwapMode;
import io.ybrid.api.bouquet.Bouquet;
import io.ybrid.api.bouquet.Service;
import io.ybrid.api.metadata.ItemType;
import io.ybrid.api.session.Command;
import io.ybrid.player.io.DataBlock;
import io.ybrid.player.io.audio.BufferStatusProvider;
import io.ybrid.player.io.audio.output.AudioOutput;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import io.ybrid.player.io.decoder.Decoder;
import io.ybrid.player.io.decoder.DecoderFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;

/**
 * This implements a Ybrid capable {@link Player}.
 *
 * See also {@link SessionClient}.
 */
public class YbridPlayer extends BasePlayer implements SessionClient, BufferStatusProvider {
    static final @NonNls Logger LOGGER = Logger.getLogger(YbridPlayer.class.getName());

    private static final double AUDIO_BUFFER_PREBUFFER = 1.5; /* [s] */

    private PlayerState playerState = PlayerState.STOPPED;

    /**
     * Creates a new instance of the player.
     *
     * @param session The {@link Session} to use for retrieval and interaction with the stream.
     * @param decoderFactory The {@link DecoderFactory} used to create a {@link Decoder} for the stream.
     * @param audioBackendFactory The {@link AudioOutputFactory} used to create a {@link AudioOutput} to interact with the host audio output.
     */
    public YbridPlayer(@NotNull Session session, @Nullable DecoderFactory decoderFactory, @NotNull AudioOutputFactory audioBackendFactory) {
        super(session, decoderFactory, audioBackendFactory);
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
    public void windToLive() throws IOException {
        executeRequestAsTransaction(Command.WIND_TO_LIVE);
    }

    @Override
    public void windTo(@NotNull Instant timestamp) throws IOException {
        executeRequestAsTransaction(Command.WIND_TO, timestamp);
    }

    @Override
    public void wind(@NotNull Duration duration) throws IOException {
        executeRequestAsTransaction(Command.WIND_BY, duration);
    }

    @Override
    public void skipForwards(ItemType itemType) throws IOException {
        executeRequestAsTransaction(Command.SKIP_FORWARD, itemType);
    }

    @Override
    public void skipBackwards(ItemType itemType) throws IOException {
        executeRequestAsTransaction(Command.SKIP_BACKWARD, itemType);
    }

    @Override
    public void swapItem(SwapMode mode) throws IOException {
        executeRequestAsTransaction(Command.SWAP_ITEM, mode);
    }

    @Override
    public void swapService(@NotNull Service service) throws IOException {
        executeRequestAsTransaction(Command.SWAP_SERVICE, service);
    }

    @Override
    public void swapToMain() throws IOException {
        executeRequestAsTransaction(Command.SWAP_TO_MAIN_SERVICE);
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
