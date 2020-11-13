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

import io.ybrid.api.Session;
import io.ybrid.api.session.Command;
import io.ybrid.api.session.Request;
import io.ybrid.api.transaction.Transaction;
import io.ybrid.player.AudioBackendFactory;
import io.ybrid.player.DecoderFactory;
import io.ybrid.player.io.audio.BufferMuxer;
import io.ybrid.player.io.audio.BufferStatusConsumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This class is used as a raw skeleton for player implementation.
 * It contains trivial methods such as basic accessors.
 *
 * This does not contain any audio processing.
 */
abstract class PlayerStub implements Player {
    protected final @NotNull Session session;
    protected final @NotNull BufferMuxer muxer;
    protected final @NotNull DecoderFactory externalDecoderFactory;
    protected final @NotNull AudioBackendFactory externalAudioBackendFactory;
    protected MetadataConsumer metadataConsumer = null;
    protected boolean autoReconnect = true;

    public PlayerStub(@NotNull Session session, @NotNull DecoderFactory externalDecoderFactory, @NotNull AudioBackendFactory externalAudioBackendFactory) {
        this.session = session;
        this.externalDecoderFactory = externalDecoderFactory;
        this.externalAudioBackendFactory = externalAudioBackendFactory;

        this.muxer = new BufferMuxer(session);
        setMetadataConsumer(null);
    }

    public boolean isAutoReconnect() {
        return autoReconnect;
    }

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

    protected void executeRequestAsTransaction(@NotNull Request request) throws IOException {
        final @NotNull Transaction transaction = session.createTransaction(request);
        final @Nullable Throwable error;

        transaction.run();
        error = transaction.getError();

        if (error == null)
            return;

        if (error instanceof IOException)
            throw (IOException)error;

        throw new IOException(error);
    }

    protected void executeRequestAsTransaction(@NotNull Command command) throws IOException {
        executeRequestAsTransaction(command.makeRequest());
    }

    protected void executeRequestAsTransaction(@NotNull Command command, @Nullable Object argument) throws IOException {
        executeRequestAsTransaction(command.makeRequest(argument));
    }

}
