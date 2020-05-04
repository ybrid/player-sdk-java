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

import java.time.Instant;

public class BufferStatus {
    private final long underruns;
    private final Instant underrunTimestamp;
    private final long overruns;
    private final Instant overrunTimestmap;
    private final double max;
    private final Instant maxTimestamp;
    private final double minAfterMax;
    private final Instant minAfterMaxTimestamp;
    private final double current;
    private final Instant currentTimestamp;

    BufferStatus(long underruns, Instant underrunTimestamp,
                 long overruns, Instant overrunTimestmap,
                 double max, Instant maxTimestamp,
                 double minAfterMax, Instant minAfterMaxTimestamp,
                 double current, Instant currentTimestamp) {
        this.underruns = underruns;
        this.underrunTimestamp = underrunTimestamp;
        this.overruns = overruns;
        this.overrunTimestmap = overrunTimestmap;
        this.max = max;
        this.maxTimestamp = maxTimestamp;
        this.minAfterMax = minAfterMax;
        this.minAfterMaxTimestamp = minAfterMaxTimestamp;
        this.current = current;
        this.currentTimestamp = currentTimestamp;
    }

    public long getUnderruns() {
        return underruns;
    }

    public Instant getUnderrunTimestamp() {
        return underrunTimestamp;
    }

    public long getOverruns() {
        return overruns;
    }

    public Instant getOverrunTimestmap() {
        return overrunTimestmap;
    }

    public double getMax() {
        return max;
    }

    public Instant getMaxTimestamp() {
        return maxTimestamp;
    }

    public double getMinAfterMax() {
        return minAfterMax;
    }

    public Instant getMinAfterMaxTimestamp() {
        return minAfterMaxTimestamp;
    }

    public double getCurrent() {
        return current;
    }

    public Instant getCurrentTimestamp() {
        return currentTimestamp;
    }
}
