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

import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

/**
 * Flags as defined for {@link Page} and {@link Packet}.
 * See RFC 3533 Section 6.
 */
public enum Flag {
    /**
     * Objects flagged with {@code BOS} signal the start of this stream.
     */
    BOS,
    /**
     * Objects flagged with {@code EOS} signal the end of this stream.
     */
    EOS,
    /**
     * Objects flagged with {@code CONTINUED} continue a previous object.
     */
    CONTINUED;

    /**
     * Convert a raw RFC 3533 bit array to a {@link EnumSet}.
     * @param raw The bits as per Ogg page header.
     * @return The resulting {@link EnumSet}.
     */
    @SuppressWarnings("MagicNumber")
    static public @NotNull EnumSet<Flag> valuesOf(byte raw) {
        final @NotNull EnumSet<Flag> ret = EnumSet.noneOf(Flag.class);

        if ((raw & 0xF8) != 0)
            throw new IllegalArgumentException("Invalid value: " + raw);

        if ((raw & 0x01) == 0x01)
            ret.add(CONTINUED);
        if ((raw & 0x02) == 0x02)
            ret.add(BOS);
        if ((raw & 0x04) == 0x04)
            ret.add(EOS);

        return ret;
    }
}
