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
import io.ybrid.player.io.muxer.ogg.PacketAdapter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public class OpusHead extends Header {
    @SuppressWarnings("StaticMethodOnlyUsedInOneClass")
    @NonNls
    static final byte[] MAGIC = "OpusHead".getBytes(StandardCharsets.UTF_8);

    private final @NotNull PacketAdapter block;
    private final @NotNull Version version;
    private final int channelCount;
    private final int preSkip;
    private final int inputSampleRate;
    private final int outputGainQ78;
    private final @NotNull ChannelMapping channelMapping;

    public OpusHead(@NotNull PacketAdapter block) {
        super(block.getSync(), block.getPlayoutInfo());
        final @NotNull byte[] raw = block.getData();

        this.block = block;
        this.version = new Version(raw[8]);
        this.channelCount = raw[9] & 0xFF;
        this.preSkip = Util.readLE16(raw, 10);
        this.inputSampleRate = Util.readLE32(raw, 12);
        this.outputGainQ78 = Util.readLE16(raw, 16);
        this.channelMapping = new ChannelMapping(channelCount, raw, 18);
    }

    public @NotNull byte[] getRaw() {
        return block.getData();
    }

    public @NotNull Version getVersion() {
        return version;
    }

    public int getChannelCount() {
        return channelCount;
    }

    public int getPreSkip() {
        return preSkip;
    }

    public int getInputSampleRate() {
        return inputSampleRate;
    }

    public int getOutputGainQ78() {
        return outputGainQ78;
    }

    public double getOutputGain() {
        //noinspection MagicNumber
        return outputGainQ78 / 256.;
    }

    public @NotNull ChannelMapping getChannelMapping() {
        return channelMapping;
    }

    @Override
    public String toString() {
        //noinspection HardCodedStringLiteral
        return "OpusHead{" +
                "version=" + version +
                ", channelCount=" + channelCount +
                ", preSkip=" + preSkip +
                ", inputSampleRate=" + inputSampleRate +
                ", outputGainQ78=" + outputGainQ78 + " (" + getOutputGain() + "dB)" +
                ", channelMapping=" + channelMapping +
                "}";
    }
}
