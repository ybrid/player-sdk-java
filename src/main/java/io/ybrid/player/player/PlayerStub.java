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

import io.ybrid.api.session.Session;
import io.ybrid.api.transaction.Request;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.api.transaction.TransactionExecutionException;
import io.ybrid.player.io.DataSourceFactory;
import io.ybrid.player.io.DataSourceFactorySelector;
import io.ybrid.player.io.audio.BufferMuxer;
import io.ybrid.player.io.audio.BufferStatusConsumer;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import io.ybrid.player.io.decoder.DecoderFactory;
import io.ybrid.player.io.decoder.DecoderFactorySelector;
import io.ybrid.player.io.decoder.DemuxerDecoderFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Logger;

/**
 * This class is used as a raw skeleton for player implementation.
 * It contains trivial methods such as basic accessors.
 *
 * This does not contain any audio processing.
 */
abstract class PlayerStub implements Player {
    static final @NonNls Logger LOGGER = Logger.getLogger(PlayerStub.class.getName());

    protected final @NotNull Session session;
    protected final @NotNull BufferMuxer muxer;
    protected final @NotNull DecoderFactorySelector decoderFactory;
    protected final @NotNull AudioOutputFactory externalAudioBackendFactory;
    protected final @NotNull DataSourceFactory dataSourceFactory;
    protected MetadataConsumer metadataConsumer = null;
    protected boolean autoReconnect = true;

    public PlayerStub(@NotNull Session session, @Nullable DecoderFactory externalDecoderFactory, @NotNull AudioOutputFactory audioOutputFactory, @Nullable DataSourceFactory dataSourceFactory) {
        this.session = session;
        this.externalAudioBackendFactory = audioOutputFactory;

        if (dataSourceFactory == null)
            dataSourceFactory = DataSourceFactorySelector.createWithDefaults();

        this.dataSourceFactory = dataSourceFactory;

        this.muxer = new BufferMuxer(session, this);

        this.decoderFactory = new DecoderFactorySelector();
        this.decoderFactory.add(new DemuxerDecoderFactory(this.decoderFactory));
        this.decoderFactory.add(new io.ybrid.player.io.codec.mp3.DecoderFactory());
        this.decoderFactory.add(new io.ybrid.player.io.codec.opus.implementation.DecoderFactory());
        this.decoderFactory.add(new io.ybrid.player.io.codec.aac.DecoderFactory());
        if (externalDecoderFactory != null)
            this.decoderFactory.add(externalDecoderFactory);

        setMetadataConsumer(null);
    }

    /**
     * Gets whether auto reconnect is enabled.
     * @return Whether auto reconnect is enabled.
     */
    public boolean isAutoReconnect() {
        return autoReconnect;
    }

    /**
     * Sets whether auto reconnect should be enabled.
     * @param autoReconnect Whether auto reconnect should be enabled.
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
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

    /**
     * Execute the given transaction on this player.
     *
     * @param transaction The transaction to execute.
     * @throws TransactionExecutionException Thrown if the transaction failed while this method was still executing.
     */
    protected void executeTransaction(@NotNull Transaction transaction) throws TransactionExecutionException {
        transaction.run();
        transaction.assertSuccess();
    }

    @Override
    public @NotNull Transaction execute(@NotNull Request<?> request) throws TransactionExecutionException {
        final @NotNull Transaction transaction = session.createTransaction(request);
        executeTransaction(transaction);
        return transaction;
    }
}
