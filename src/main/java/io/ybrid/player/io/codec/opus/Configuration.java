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

package io.ybrid.player.io.codec.opus;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Frame configuration as defined by RFC 6716 Section 3.1 Table 2.
 */
public enum Configuration {
    SILK_ONLY_NB_10ms(0, Mode.SILK_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_10),
    SILK_ONLY_NB_20ms(1, Mode.SILK_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_20),
    SILK_ONLY_NB_40ms(2, Mode.SILK_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_40),
    SILK_ONLY_NB_60ms(3, Mode.SILK_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_60),
    SILK_ONLY_MB_10ms(4, Mode.SILK_ONLY, Bandwidth.MB, FrameSize.FRAME_SIZE_10),
    SILK_ONLY_MB_20ms(5, Mode.SILK_ONLY, Bandwidth.MB, FrameSize.FRAME_SIZE_20),
    SILK_ONLY_MB_40ms(6, Mode.SILK_ONLY, Bandwidth.MB, FrameSize.FRAME_SIZE_40),
    SILK_ONLY_MB_60ms(7, Mode.SILK_ONLY, Bandwidth.MB, FrameSize.FRAME_SIZE_60),
    SILK_ONLY_WB_10ms(8, Mode.SILK_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_10),
    SILK_ONLY_WB_20ms(9, Mode.SILK_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_20),
    SILK_ONLY_WB_40ms(10, Mode.SILK_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_40),
    SILK_ONLY_WB_60ms(11, Mode.SILK_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_60),
    HYBRID_SWB_10ms(12, Mode.HYBRID, Bandwidth.SWB, FrameSize.FRAME_SIZE_10),
    HYBRID_SWB_20ms(13, Mode.HYBRID, Bandwidth.SWB, FrameSize.FRAME_SIZE_20),
    HYBRID_FB_10ms(14, Mode.HYBRID, Bandwidth.FB, FrameSize.FRAME_SIZE_10),
    HYBRID_FB_20ms(15, Mode.HYBRID, Bandwidth.FB, FrameSize.FRAME_SIZE_20),
    CELT_ONLY_NB_2_5ms(16, Mode.CELT_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_2_5),
    CELT_ONLY_NB_5ms(17, Mode.CELT_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_5),
    CELT_ONLY_NB_10ms(18, Mode.CELT_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_10),
    CELT_ONLY_NB_20ms(19, Mode.CELT_ONLY, Bandwidth.NB, FrameSize.FRAME_SIZE_20),
    CELT_ONLY_WB_2_5ms(20, Mode.CELT_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_2_5),
    CELT_ONLY_WB_5ms(21, Mode.CELT_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_5),
    CELT_ONLY_WB_10ms(22, Mode.CELT_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_10),
    CELT_ONLY_WB_20ms(23, Mode.CELT_ONLY, Bandwidth.WB, FrameSize.FRAME_SIZE_20),
    CELT_ONLY_SWB_2_5ms(24, Mode.CELT_ONLY, Bandwidth.SWB, FrameSize.FRAME_SIZE_2_5),
    CELT_ONLY_SWB_5ms(25, Mode.CELT_ONLY, Bandwidth.SWB, FrameSize.FRAME_SIZE_5),
    CELT_ONLY_SWB_10ms(26, Mode.CELT_ONLY, Bandwidth.SWB, FrameSize.FRAME_SIZE_10),
    CELT_ONLY_SWB_20ms(27, Mode.CELT_ONLY, Bandwidth.SWB, FrameSize.FRAME_SIZE_20),
    CELT_ONLY_FB_2_5ms(28, Mode.CELT_ONLY, Bandwidth.FB, FrameSize.FRAME_SIZE_2_5),
    CELT_ONLY_FB_5ms(29, Mode.CELT_ONLY, Bandwidth.FB, FrameSize.FRAME_SIZE_5),
    CELT_ONLY_FB_10ms(30, Mode.CELT_ONLY, Bandwidth.FB, FrameSize.FRAME_SIZE_10),
    CELT_ONLY_FB_20ms(31, Mode.CELT_ONLY, Bandwidth.FB, FrameSize.FRAME_SIZE_20);

    private static final @NotNull Map<Integer, Configuration> values = new HashMap<>();

    private final int number;
    private final @NotNull Mode mode;
    private final @NotNull Bandwidth bandwidth;
    private final @NotNull FrameSize frameSize;

    static {
        for (final @NotNull Configuration configuration : values())
            values.put(configuration.number, configuration);
    }

    /**
     * Gets a configuration based on it's number.
     * For valid configuration numbers see RFC 6716 Section 3.1 Table 2.
     * @param val The configuration number.
     * @return The corresponding configuration.
     */
    public static @NotNull Configuration valueOf(int val) {
        return values.get(val);
    }

    Configuration(int number, @NotNull Mode mode, @NotNull Bandwidth bandwidth, @NotNull FrameSize frameSize) {
        this.number = number;
        this.mode = mode;
        this.bandwidth = bandwidth;
        this.frameSize = frameSize;
    }

    /**
     * Gets the {@link Mode} used by the given configuration.
     * @return The corresponding mode.
     */
    @Contract(pure = true)
    public @NotNull Mode getMode() {
        return mode;
    }

    /**
     * Gets the audio {@link Bandwidth} provided by the given configuration.
     * @return the corresponding audio bandwidth.
     */
    @Contract(pure = true)
    public @NotNull Bandwidth getBandwidth() {
        return bandwidth;
    }

    /**
     * Gets the {@link FrameSize} for frames of this configuration.
     * @return The corresponding frame size.
     */
    @Contract(pure = true)
    public @NotNull FrameSize getFrameSize() {
        return frameSize;
    }
}
