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

import io.ybrid.player.io.mapping.Mapping;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class StreamInfo {
    private final @NotNull Mapping<?, ?> mapping;

    public StreamInfo(@NotNull Mapping<?, ?> mapping) {
        this.mapping = mapping;
    }

    public @NotNull Mapping<?, ?> getMapping() {
        return mapping;
    }

    @Contract(pure = true)
    public @NotNull StreamUsage getPrimaryStreamUsage() {
        return mapping.getPrimaryStreamUsage();
    }

    @Contract(pure = true)
    public @NotNull Set<StreamUsage> getStreamUsage() {
        return mapping.getStreamUsage();
    }

    /**
     * Return the content-type of the stream.
     *
     * @return The content-type or {@code null}.
     */
    @Nullable String getContentType() {
        return mapping.getContentType();
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "StreamInfo{" +
                "mapping=" + mapping +
                ", getPrimaryStreamUsage()=" + getPrimaryStreamUsage() +
                ", getStreamUsage()=" + getStreamUsage() +
                ", getContentType()=" + getContentType() +
                "}";
    }
}
