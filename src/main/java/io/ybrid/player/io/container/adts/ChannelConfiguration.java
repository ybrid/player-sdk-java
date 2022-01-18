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

import java.util.NoSuchElementException;

public enum ChannelConfiguration {
    MONO(1, 1),
    STEREO(2, 2),
    FC_FL_FL(3, 3),
    FC_FL_FR_BC(4, 4),
    FC_FL_FR_BL_BR(5, 5),
    FC_FL_FR_BL_BR_LFE(6, 6),
    FC_FL_FR_SL_SR_BL_BR_LFE(7, 8)
    ;
    private final int value;
    private final int count;

    ChannelConfiguration(int value, int count) {
        this.value = value;
        this.count = count;
    }

    static @NotNull ChannelConfiguration fromWire(int value) {
        for (final @NotNull ChannelConfiguration configuration : values()) {
            if (configuration.value == value)
                return configuration;
        }
        throw new NoSuchElementException();
    }


    public int getCount() {
        return count;
    }
}
