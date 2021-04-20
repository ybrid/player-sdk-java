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

package io.ybrid.player.io.audio.analysis.result;

import io.ybrid.player.io.audio.SignalInformation;
import io.ybrid.player.io.audio.analysis.Util;
import org.jetbrains.annotations.ApiStatus;

/**
 * This interface is implemented by classes providing a signal analysis result.
 */
@ApiStatus.Experimental
public interface Result extends SignalInformation {
    /**
     * Gets the minimum value in the signal as a {@code short}.
     *
     * @return The minimum value or 0 if there are no samples.
     */
    short getMinAsShort();

    /**
     * Gets the maximum value in the signal as a {@code short}.
     *
     * @return The maximum value or 0 if there are no samples.
     */
    short getMaxAsShort();

    /**
     * Gets the minimum value in the signal as a {@code double}.
     *
     * @return The minimum value or 0 if there are no samples.
     */
    default double getMinAsDouble() {
        return Util.shortToDouble(getMinAsShort());
    }

    /**
     * Gets the maximum value in the signal as a {@code double}.
     *
     * @return The maximum value or 0 if there are no samples.
     */
    default double getMaxAsDouble() {
        return Util.shortToDouble(getMaxAsShort());
    }

    /**
     * Gets the DC offset as a {@code double}.
     *
     * @return The DC offset or 0 if there are no samples.
     */
    double getDCDouble();

    /**
     * Gets the power of the signal in [dBFS].
     *
     * @return The power of the signal in [dBFS]
     */
    double getPowerIndB();
}
