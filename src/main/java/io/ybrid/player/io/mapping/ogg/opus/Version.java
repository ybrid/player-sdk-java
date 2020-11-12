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

package io.ybrid.player.io.mapping.ogg.opus;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Version {
    /**
     * As defined by RFC 7845.
     */
    public static final Version VERSION_1 = new Version(0, 1);

    private final int major;
    private final int minor;

    private Version(int major, int minor) {
        this.major = major;
        this.minor = minor;
    }

    public Version(byte raw) {
        //noinspection MagicNumber
        this((raw & 0xF0) >> 4, raw & 0x0F);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public boolean isCompatible(@NotNull Version other) {
        return getMajor() == other.getMajor() && getMinor() <= other.getMinor();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major &&
                minor == version.minor;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, 9180981098021L);
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "Version{" +
                "major=" + major +
                ", minor=" + minor +
                "}";
    }
}
