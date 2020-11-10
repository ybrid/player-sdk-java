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

package io.ybrid.player.io.container.ogg;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * This is a helper class representing a granule position as defined by RFC 3533 Section 6.
 * The exact meaning of a granule position depends on the mapping used.
 */
public class GranularPosition {
    /**
     * The invalid position.
     */
    static public final @NotNull GranularPosition INVALID = new GranularPosition(-1);

    private final long raw;

    private GranularPosition(long raw) {
        this.raw = raw;
    }

    /**
     * Constructs a granular position based on the binary format given in
     * RFC 3533 Section 6.
     * @param raw The byte array to exact the value from.
     * @param offset The offset of the value in bytes.
     */
    @Contract(pure = true)
    public GranularPosition(@NotNull byte[] raw, int offset) {
        this.raw = Util.readLE64(raw, offset);
    }

    /**
     * Checks whether the value is valid.
     * This is equal to calling {@link #equals(Object)} on the object passing {@link #INVALID} as argument.
     * @return Whether the value is valid.
     */
    public boolean isValid() {
        return raw != -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GranularPosition that = (GranularPosition) o;
        return raw == that.raw;
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, 928308210218021809L);
    }

    @Override
    public String toString() {
        return "GranularPosition{" +
                "raw=" + raw +
                '}';
    }
}
