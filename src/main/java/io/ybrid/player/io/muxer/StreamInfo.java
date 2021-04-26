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

package io.ybrid.player.io.muxer;

import io.ybrid.api.util.MediaType;
import io.ybrid.player.io.mapping.Mapping;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * This class represent general information about a stream.
 * The main purpose of this class is to provide information required to
 * select or deselect streams for demuxing.
 * Detailed metadata about a stream can be obtained via the {@link Mapping} once
 * the stream is being demuxed.
 */
public class StreamInfo {
    private final @NotNull Mapping<?, ?> mapping;

    /**
     * Main constructor. This is internal API and should not be used.
     * @param mapping The mapping for this stream.
     */
    @ApiStatus.Internal
    public StreamInfo(@NotNull Mapping<?, ?> mapping) {
        this.mapping = mapping;
    }

    /**
     * Gets the active mapping for this stream.
     * @return The active mapping.
     */
    public @NotNull Mapping<?, ?> getMapping() {
        return mapping;
    }

    /**
     * Gets the primary usage of this stream.
     * This should be the primary mean to test if a stream should be selected for demuxing.
     * @return The primary usage of the stream.
     */
    @Contract(pure = true)
    public @NotNull StreamUsage getPrimaryStreamUsage() {
        return mapping.getPrimaryStreamUsage();
    }

    /**
     * Gets the usages of this stream.
     * This will include the primary usage as returned by {@link #getPrimaryStreamUsage()}.
     * @return The set of usages for this stream.
     */
    @Contract(pure = true)
    public @NotNull Set<StreamUsage> getStreamUsage() {
        return mapping.getStreamUsage();
    }

    /**
     * Return the {@link MediaType} of the stream.
     *
     * @return The {@link MediaType} or {@code null}.
     */
    @Nullable MediaType getMediaType() {
        return mapping.getMediaType();
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "StreamInfo{" +
                "mapping=" + mapping +
                ", getPrimaryStreamUsage()=" + getPrimaryStreamUsage() +
                ", getStreamUsage()=" + getStreamUsage() +
                ", getMediaType()=" + getMediaType() +
                "}";
    }
}
