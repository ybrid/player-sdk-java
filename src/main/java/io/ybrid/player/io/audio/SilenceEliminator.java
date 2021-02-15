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

package io.ybrid.player.io.audio;

import io.ybrid.player.io.PCMDataBlock;
import io.ybrid.player.io.PCMDataSource;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * This class is a {@link Skipper} that removes silence at start of a stream.
 */
public final class SilenceEliminator extends Skipper {
    /**
     * This enum contains different types of silence.
     */
    public enum SilenceType {
        /**
         * Digital silence is a section of audio that only contains samples with exactly no energy.
         */
        DIGITAL(0),
        /**
         * Analog silence represents a section of audio that has a very low energy level.
         */
        ANALOG(32);

        private final short maxValue;

        SilenceType(int maxValue) {
            this.maxValue = (short) maxValue;
        }

        /**
         * Gets the maximum value a sample may have with this type of silence.
         * @return The maximum sample value.
         */
        @Contract(pure = true)
        public short getMaxValue() {
            return maxValue;
        }
    }

    private final @NotNull SilenceType silenceType;

    /**
     * Main constructor.
     *
     * @param backend The backend to use.
     * @param silenceType The type of silence to detect.
     */
    @Contract(pure = true)
    public SilenceEliminator(@NotNull PCMDataSource backend, @NotNull SilenceType silenceType) {
        super(backend);
        this.silenceType = silenceType;
    }

    @Override
    void examine(@NotNull PCMDataBlock block) {
        if (!preSkipDone) {
            final short maxValue = silenceType.getMaxValue();

            final short[] data = block.getData();
            final int numberOfChannels = block.getNumberOfChannels();

            for (int toSkip = 0; toSkip < data.length; toSkip += numberOfChannels) {
                boolean isSilence = true;
                for (int i = 0; i < numberOfChannels; i++) {
                    if (data[toSkip + i] < -maxValue || data[toSkip + i] > maxValue) {
                        isSilence = false;
                        break;
                    }
                }
                if (isSilence) {
                    preSkip++;
                } else {
                    break;
                }
            }
        }
    }
}
