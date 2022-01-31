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
 * This represents a possible sampling frequency.
 */
public enum SamplingFrequency {
    FREQUENCY_96000(0, 96000),
    FREQUENCY_88200(1, 88200),
    FREQUENCY_64000(2, 64000),
    FREQUENCY_48000(3, 48000),
    FREQUENCY_44100(4, 44100),
    FREQUENCY_32000(5, 32000),
    FREQUENCY_24000(6, 24000),
    FREQUENCY_22050(7, 22050),
    FREQUENCY_16000(8, 16000),
    FREQUENCY_12000(9, 12000),
    FREQUENCY_11025(10, 11025),
    FREQUENCY_8000(11, 8000),
    FREQUENCY_7350(12, 7350);

    private final int value;
    private final int frequency;

    SamplingFrequency(int value, int frequency) {
        this.value = value;
        this.frequency = frequency;
    }

    /**
     * Get the frequency from the given value.
     * @param value The value as used by ADTS.
     * @return The corresponding frequency.
     */
    static @NotNull SamplingFrequency fromWire(int value) {
        for (final @NotNull SamplingFrequency frequency : values()) {
            if (frequency.value == value)
                return frequency;
        }
        throw new NoSuchElementException();
    }

    /**
     * Get the raw ADTS value from the given frequency.
     * @return The value as used by ADTS.
     */
    @Contract(pure = true)
    public int toWire() {
        return value;
    }

    /**
     * Get the actual sampling frequency.
     * @return The frequency in [Hz].
     */
    @Contract(pure = true)
    public int getFrequency() {
        return frequency;
    }
}
