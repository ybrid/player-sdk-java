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

package io.ybrid.player.io;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * This helper class provides constants for common media types.
 */
@SuppressWarnings("HardCodedStringLiteral")
public final class MediaType {
    /* --------[ Internal Media Types ]-------- */
    /**
     * PCM stream as stream of blocks of {@code short[]}.
     */
    public static final @NotNull io.ybrid.api.util.MediaType PCM_STREAM_SHORT = new io.ybrid.api.util.MediaType("!_block-stream/pcm-java-short-array");
    /**
     * Demuxed Opus stream.
     */
    public static final @NotNull io.ybrid.api.util.MediaType BLOCK_STREAM_OPUS = new io.ybrid.api.util.MediaType("!_block-stream/opus");

    /* --------[ Methods ]-------- */
    @ApiStatus.Internal
    public static boolean isInternal(@NotNull io.ybrid.api.util.MediaType mediaType) {
        return mediaType.toString().startsWith("!_");
    }
}
