/*
 * Copyright (c) 2022 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.container.adts;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.NoSuchElementException;

/**
 * MPEG-4 Audio Object Type
 */
public enum AudioObjectType {
    AAC_MAIN(1),
    AAC_LC(2),
    AAC_SSR(3),
    AAC_LTP(4);

    private final int value;

    AudioObjectType(int value) {
        this.value = value;
    }

    /**
     * Get the Audio Object Type from the given value.
     * @param value The value as used by ADTS (zero meaning main profile).
     * @return The corresponding Audio Object Type.
     */
    static @NotNull AudioObjectType fromWire(int value) {
        value++; // as per specs

        for (final @NotNull AudioObjectType audioObjectType : values()) {
            if (audioObjectType.value == value)
                return audioObjectType;
        }
        throw new NoSuchElementException();
    }

    /**
     * Get the raw ADTS value from the given Audio Object Type.
     * @return The value as used by ADTS (zero meaning main).
     */
    @Contract(pure = true)
    public int toWire() {
        return value - 1;
    }
}
