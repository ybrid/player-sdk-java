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

import io.ybrid.api.Bouquet;
import io.ybrid.api.CapabilitySet;
import io.ybrid.api.Metadata;
import io.ybrid.api.PlayoutInfo;
import org.jetbrains.annotations.NotNull;

/**
 * This interface is implemented by classes that can consume Ybrid {@link Metadata}.
 */
public interface MetadataConsumer {
    /**
     * This function is called by the {@link MetadataProvider} when there is new {@link Metadata}
     * to be consumed.
     *
     * @param metadata The {@link Metadata} to consume.
     */
    void onMetadataChange(@NotNull Metadata metadata);

    /**
     * This function is called by the {@link MetadataProvider} when the state of the player changed.
     * @param playerState The new state of the player.
     */
    void onPlayerStateChange(@NotNull PlayerState playerState);

    /**
     * This function is called by the {@link MetadataProvider} when the set of the player's capabilities changed.
     * @param capabilities The new set of capabilities.
     */
    void onCapabilitiesChange(@NotNull CapabilitySet capabilities);

    /**
     * This function is called by the {@link MetadataProvider} when the playout information of the session changed.
     * @param playoutInfo The new playout information of the session.
     */
    void onPlayoutInfoChange(@NotNull PlayoutInfo playoutInfo);

    /**
     * This function is called by the {@link MetadataProvider} when the available bouquet changed.
     * @param bouquet The new bouquet.
     */
    void onBouquetChange(@NotNull Bouquet bouquet);
}
