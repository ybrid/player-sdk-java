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

import io.ybrid.player.io.audio.analysis.Util;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Range;

import java.util.Locale;

@ApiStatus.Experimental
public class Channel implements Result {
    private final int sampleRate;
    private final int frames;
    private final short min;
    private final short max;
    private final double dc;
    private final double power;

    Channel(int sampleRate, int channels, int channelIndex, short[] data) {
        if ((data.length % channels) != 0)
            throw new IllegalArgumentException();

        this.sampleRate = sampleRate;
        this.frames = data.length / channels;

        if (data.length == 0) {
            min = 0;
            max = 0;
            dc = 0.;
            power = 0.;
        } else {
            short currentMin = data[channelIndex];
            short currentMax = data[channelIndex];
            double currentDC = 0.;
            double currentPower = 0.;

            for (int i = channelIndex; i < data.length; i += channels) {
                final short value = data[i];
                final double valueAsDouble = Util.shortToDouble(value);

                if (value < currentMin)
                    currentMin = value;
                if (value > currentMax)
                    currentMax = value;

                currentDC += valueAsDouble;
                currentPower += Math.pow(valueAsDouble, 2);
            }

            min = currentMin;
            max = currentMax;
            dc = currentDC / (double) frames;

            power = Math.sqrt(currentPower / (double) frames);
        }
    }

    @Override
    public @Range(from = 1, to = Integer.MAX_VALUE) int getSampleRate() {
        return sampleRate;
    }

    @Override
    public @Range(from = 0, to = Integer.MAX_VALUE) int getLengthAsFrames() {
        return frames;
    }

    @Override
    public short getMinAsShort() {
        return min;
    }

    @Override
    public short getMaxAsShort() {
        return max;
    }

    @Override
    public double getDCDouble() {
        return dc;
    }

    @Override
    public double getPowerIndB() {
        return Util.powerTodB(power);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public String toString() {
        final String length = String.format(Locale.ROOT, "%d frames (%s)", getLengthAsFrames(), getLength().toString());
        final String peaks = String.format(Locale.ROOT, "[%.4f, %.4f]", getMinAsDouble(), getMaxAsDouble());
        final String DC = String.format(Locale.ROOT, "%.4f", dc);
        final String powerIndB;
        if (power > 0) {
            powerIndB = String.format(Locale.ROOT, "%.4fdB", getPowerIndB());
        } else {
            powerIndB = "-InfdB";
        }

        return "Channel{" +
                "sampleRate=" + getSampleRate() + "Hz" +
                ", length=" + length +
                ", peaks=" + peaks +
                ", dc=" + DC +
                ", power=" + powerIndB +
                "}";
    }
}
