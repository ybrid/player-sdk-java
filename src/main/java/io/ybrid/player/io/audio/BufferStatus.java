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

package io.ybrid.player.io.audio;

import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;

/**
 * This class is used to hold a state of the {@link Buffer}.
 */
final public class BufferStatus implements Serializable {
    private static final long serialVersionUID = -3172756405966691712L;

    private final long underruns;
    private final Instant underrunTimestamp;
    private final long overruns;
    private final Instant overrunTimestamp;
    private final double max;
    private final Instant maxTimestamp;
    private final double minAfterMax;
    private final Instant minAfterMaxTimestamp;
    private final double current;
    private final Instant currentTimestamp;
    private final long currentSamplesRead;
    private final long currentSamplesForwarded;

    BufferStatus(long underruns, Instant underrunTimestamp,
                 long overruns, @Nullable Instant overrunTimestamp,
                 double max, @Nullable Instant maxTimestamp,
                 double minAfterMax, @Nullable Instant minAfterMaxTimestamp,
                 double current, @Nullable Instant currentTimestamp, long currentSamplesRead, long currentSamplesForwarded) {
        this.underruns = underruns;
        this.underrunTimestamp = underrunTimestamp;
        this.overruns = overruns;
        this.overrunTimestamp = overrunTimestamp;
        this.max = max;
        this.maxTimestamp = maxTimestamp;
        this.minAfterMax = minAfterMax;
        this.minAfterMaxTimestamp = minAfterMaxTimestamp;
        this.current = current;
        this.currentTimestamp = currentTimestamp;
        this.currentSamplesRead = currentSamplesRead;
        this.currentSamplesForwarded = currentSamplesForwarded;
    }

    /**
     * Get number total of underruns.
     * @return Number of underruns.
     */
    public long getUnderruns() {
        return underruns;
    }

    /**
     * Get time of last underrun.
     * @return Time of last underrun or null if none.
     */
    @Nullable
    public Instant getUnderrunTimestamp() {
        return underrunTimestamp;
    }

    /**
     * Get number of total overruns.
     * @return Number of overruns.
     */
    public long getOverruns() {
        return overruns;
    }

    /**
     * Get time of last overrun.
     * @return Time of last overrun or null if none.
     */
    @Nullable
    public Instant getOverrunTimestamp() {
        return overrunTimestamp;
    }

    /**
     * Get maximum buffer fill ever reached.
     * @return Maximum buffer fill.
     */
    public double getMax() {
        return max;
    }

    /**
     * Get time of maximum buffer fill.
     * @return Time of maximum buffer fill or none if never filled.
     */
    @Nullable
    public Instant getMaxTimestamp() {
        return maxTimestamp;
    }

    /**
     * Get minimum buffer fill since last time max was reached.
     * @return Minimum buffer fill.
     */
    public double getMinAfterMax() {
        return minAfterMax;
    }

    /**
     * Get time of minimum buffer fill. This is always later or equal to {@link #getMaxTimestamp()}.
     * @return Time of last minimum buffer fill or null if never set.
     */
    @Nullable
    public Instant getMinAfterMaxTimestamp() {
        return minAfterMaxTimestamp;
    }

    /**
     * Get the current buffer fill.
     * This value is recorded at {@link #getCurrentTimestamp()}.
     * @return The current buffer fill.
     */
    public double getCurrent() {
        return current;
    }

    /**
     * Gets the current read clock of the buffer.
     * The read clock represents the number of samples read into the buffer.
     * <P>
     * The read clock is monotonous. It can <b>not</b> be converted into units of SI-time as the sample rate
     * is not fixed.
     * @return The read clock.
     * @see #getCurrentSamplesForwarded()
     */
    public long getCurrentSamplesRead() {
        return currentSamplesRead;
    }

    /**
     * Gets the current forward clock of the buffer.
     * The forward clock represents the number of samples forwarded by the buffer.
     * This is the amount of samples that have been read from the buffer.
     * <P>
     * The read clock is monotonous. It can <b>not</b> be converted into units of SI-time as the sample rate
     * is not fixed.
     * @return The forward clock.
     * @see #getCurrentSamplesRead()
     */
    public long getCurrentSamplesForwarded() {
        return currentSamplesForwarded;
    }

    /**
     * Get time of when current buffer fill was recorded.
     * @return Time of record or null if buffer was never filled.
     */
    @Nullable
    public Instant getCurrentTimestamp() {
        return currentTimestamp;
    }

    @NonNls
    @Override
    public String toString() {
        return "BufferStatus{" +
                "Current = " + current +
                " clock: " + currentSamplesRead + " -> " + currentSamplesForwarded + " delay: " + (currentSamplesRead - currentSamplesForwarded) + "[" + currentTimestamp + "]" +
                ", Max = " + max + "[" + maxTimestamp + "]" +
                ", MinAfterMax = " + minAfterMax + "[" + minAfterMaxTimestamp + "]" +
                ", Overruns = " + overruns + "[" + overrunTimestamp + "]" +
                ", Underruns = " + underruns + "[" + underrunTimestamp + "]" +
                '}';
    }
}
