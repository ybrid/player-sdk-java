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

import org.jetbrains.annotations.NotNull;

public class Header {
    private final @NotNull MPEGVersion mpegVersion;
    private final @NotNull Layer layer;
    private final boolean protection;
    private final @NotNull AudioObjectType audioObjectType;
    private final @NotNull SamplingFrequency samplingFrequency;
    private final @NotNull ChannelConfiguration channelConfiguration;
    private final int frameLength;
    private final int frameCount;

    public Header(byte @NotNull [] data) {
        if (data.length < 7)
            throw new IllegalArgumentException("Too little data");

        //noinspection MagicNumber
        if ((data[0] & 0xFF) != 0xFF || (data[1] & 0xF0) != 0xF0)
            throw new IllegalArgumentException("Bad Sync code");

        mpegVersion = MPEGVersion.fromWire((data[1] >> 3) & 0x1);
        layer = Layer.fromWire((data[1] >> 1) & 0x3);
        protection = (data[1] & 0x1) == 0x0;
        audioObjectType = AudioObjectType.fromWire((data[2] >> 6) & 0x3);
        samplingFrequency = SamplingFrequency.fromWire((data[2] >> 2) & 0xF);
        channelConfiguration = ChannelConfiguration.fromWire(((data[2] << 2) & 0x4) + ((data[3] >> 6) & 0x3));

        {
            int fl;
            fl = data[3] & 0x3;
            fl <<= 8;
            fl |= data[4] & 0xFF;
            fl <<= 3;
            fl |= (data[5] >> 5) & 0x7;

            frameLength = fl;
        }

        frameCount = (data[6] & 0x3) + 1;
    }

    public @NotNull AudioObjectType getAudioObjectType() {
        return audioObjectType;
    }

    public @NotNull SamplingFrequency getSamplingFrequency() {
        return samplingFrequency;
    }

    public @NotNull ChannelConfiguration getChannelConfiguration() {
        return channelConfiguration;
    }

    public int getFrameCount() {
        return frameCount;
    }

    public int getFrameLength() {
        return frameLength;
    }

    @Override
    public String toString() {
        return "Header{" +
                "mpegVersion=" + mpegVersion +
                ", layer=" + layer +
                ", protection=" + protection +
                ", audioObjectType=" + audioObjectType +
                ", samplingFrequency=" + samplingFrequency +
                ", channelConfiguration=" + channelConfiguration +
                ", frameLength=" + frameLength +
                ", frameCount=" + frameCount +
                '}';
    }
}
