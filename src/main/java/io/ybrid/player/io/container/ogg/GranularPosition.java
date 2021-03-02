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

import java.math.BigInteger;
import java.util.Objects;

/**
 * This is a helper class representing a granule position as defined by RFC 3533 Section 6.
 * The exact meaning of a granule position depends on the mapping used.
 */
public final class GranularPosition {
    /**
     * The invalid position.
     */
    static public final @NotNull GranularPosition INVALID = new GranularPosition(BigInteger.valueOf(-1));

    private final BigInteger raw;

    private GranularPosition(@NotNull BigInteger raw) {
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
        final @NotNull byte[] buf = new byte[9];
        buf[0] = 0;

        for (int i = 0; i < 8; i++) {
            buf[8 - i] = raw[offset + i];
        }

        this.raw = new BigInteger(buf);
    }

    /**
     * Adds a given amount to a GranularPosition.
     * @param val The amount to add.
     * @return The new GranularPosition or {@link #INVALID}.
     * @throws IllegalArgumentException Thrown if {@code val} is invalid.
     */
    @Contract(pure = true)
    public @NotNull GranularPosition add(long val) throws IllegalArgumentException {
        if (val < 0)
            throw new IllegalArgumentException("val is less than zero: " + val);

        if (this.isValid())
            return new GranularPosition(raw.add(BigInteger.valueOf(val)));

        return INVALID;
    }

    /**
     * Subtracts a given amount to a GranularPosition.
     * @param val The amount to subtract.
     * @return The new GranularPosition or {@link #INVALID}.
     * @throws IllegalArgumentException Thrown if {@code val} is invalid.
     */
    @Contract(pure = true)
    public @NotNull GranularPosition subtract(long val) throws IllegalArgumentException {
        if (val < 0)
            throw new IllegalArgumentException("val is less than zero: " + val);

        if (this.isValid()) {
            final @NotNull BigInteger n = raw.subtract(BigInteger.valueOf(val));
            if (n.signum() < 0)
                throw new IllegalArgumentException("val is too big for subtraction: " + val);

            return new GranularPosition(n);
        }

        return INVALID;
    }

    /**
     * Checks whether the value is valid.
     * This is equal to calling {@link #equals(Object)} on the object passing {@link #INVALID} as argument.
     * @return Whether the value is valid.
     */
    public boolean isValid() {
        return raw.signum() >= 0;
    }

    /**
     * Gets the GranularPosition in units of a target clock.
     * @param outputClockFrequency The target clock's frequency.
     * @param inputClockFrequency The source clock's frequency as provided by the Mapping.
     * @return The value of the GranularPosition in target units.
     */
    public long get(long outputClockFrequency, long inputClockFrequency) {
        if (!isValid())
            throw new IllegalArgumentException("Can not get value from invalid GranularPosition");
        return raw.multiply(BigInteger.valueOf(outputClockFrequency)).divide(BigInteger.valueOf(inputClockFrequency)).longValue();
    }

    /**
     * Compares this to {@code other}. Returns whether this is less or equal than {@code other}.
     * @param other The other GranularPosition to compare to.
     * @return The result of the comparison.
     */
    public boolean isLessOrEqualThan(@NotNull GranularPosition other) {
        if (this == other || (!isValid() && !other.isValid()))
            return true;

        if (!isValid() || !other.isValid())
            throw new IllegalArgumentException("One but not both GranularPosition are not valid: this is " + this + " and the other is " + other);

        return raw.compareTo(other.raw) <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GranularPosition that = (GranularPosition) o;
        return raw.equals(that.raw);
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
