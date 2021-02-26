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

import io.ybrid.player.io.container.ogg.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ChannelMapping {
    public enum Family {
        RTP(0),
        VORBIS(1),
        AMBISONICS_2(2), // RFC 8486 Section 3.1
        AMBISONICS_3(3), // RFC 8486 Section 3.2
        UNDEFINED_MEANING(255);

        private static final @NotNull Map<@NotNull Byte, Family> values = new HashMap<>();

        private final byte raw;

        static {
            for (final @NotNull Family family : Family.values())
                values.put(family.getRaw(), family);
        }

        Family(int raw) {
            this.raw = (byte)raw;
        }

        public byte getRaw() {
            return raw;
        }

        public static @NotNull Family valueOf(byte raw) {
            final @Nullable Family ret = values.get(raw);
            if (ret == null)
                throw new IllegalArgumentException("Unknown raw value: " + raw);
            return ret;
        }

        @Override
        public String toString() {
            //noinspection HardCodedStringLiteral,MagicNumber
            return String.format(Locale.ROOT, "Family{name=%s, raw=0x%02x}", super.toString(), raw & 0xFF);
        }
    }

    private final int outputChannelCount;
    private final @NotNull Family family;
    private final int streamCount;
    private final int coupledStreamCount;
    private final @NotNull byte[] matrix;

    public ChannelMapping(int outputChannelCount, @NotNull byte[] raw, int offset) {
        this.outputChannelCount = outputChannelCount;
        this.family = Family.valueOf(raw[offset]);

        if (outputChannelCount < 1)
            throw new IllegalArgumentException("Output channel count must not be < 1 but is " + outputChannelCount);

        switch (this.family) {
            case RTP:
                this.streamCount = 1;
                switch (outputChannelCount) {
                    case 1:
                        this.coupledStreamCount = 0;
                        this.matrix = new byte[]{0};
                        break;
                    case 2:
                        this.coupledStreamCount = 1;
                        this.matrix = new byte[]{0, 1};
                        break;
                    default:
                        throw new IllegalArgumentException("outputChannelCount must not be > 2 but is " + outputChannelCount);
                }
                break;
            case VORBIS:
            case UNDEFINED_MEANING:
            case AMBISONICS_2:
            case AMBISONICS_3:
                this.streamCount = raw[offset + 1] & 0xFF;
                this.coupledStreamCount = raw[offset + 2] & 0xFF;
                this.matrix = Util.extractBytes(raw, offset + 3, outputChannelCount);
                break;
            default:
                throw new IllegalArgumentException("Unsupported mapping family: " + this.family);
        }
    }

    public int getOutputChannelCount() {
        return outputChannelCount;
    }

    public @NotNull Family getFamily() {
        return family;
    }

    public int getStreamCount() {
        return streamCount;
    }

    public int getCoupledStreamCount() {
        return coupledStreamCount;
    }

    public byte[] getMatrix() {
        return matrix;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "ChannelMapping{" +
                "outputChannelCount=" + outputChannelCount +
                ", family=" + family +
                ", streamCount=" + streamCount +
                ", coupledStreamCount=" + coupledStreamCount +
                ", matrix=" + Arrays.toString(matrix) +
                "}";
    }
}
