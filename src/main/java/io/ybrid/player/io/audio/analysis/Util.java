/*
 * Copyright (c) 2021 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io.audio.analysis;

import org.jetbrains.annotations.Contract;

/**
 * This class contains helper methods for signal analysis.
 */
public final class Util {
    /**
     * Converts a {@code short} to a {@code double} with correct scaling.
     * @param val The {@code short} to convert.
     * @return The resulting {@code double}.
     */
    @Contract(pure = true)
    static public double shortToDouble(short val) {
        if (val >= 0) {
            return val/(double)Short.MAX_VALUE;
        } else {
            return -(val/(double)Short.MIN_VALUE);
        }
    }

    /**
     * Converts a power factor to [dB].
     * @param power The power level to convert.
     * @return The resulting value in [dB].
     */
    @Contract(pure = true)
    static public double powerTodB(double power) {
        //noinspection MagicNumber
        return 20. * Math.log10(power);
    }
}
