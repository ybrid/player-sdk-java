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

package io.ybrid.player.io.container.ogg;

import org.jetbrains.annotations.NotNull;

import java.util.zip.Checksum;

/**
 * CRC-32 as used by Ogg [RFC 3533]:
 * Generator {@code 0x04c11db7}, initial and final of {@code 0x0}.
 */
public final class CRC implements Checksum {
    private static final int CRC_LOOKUP_SIZE = 256;
    private static final @NotNull int[] CRC_LOOKUP = buildLookup();
    private int ret;

    @SuppressWarnings("MagicNumber")
    private static @NotNull int[] buildLookup() {
        final @NotNull int[] table = new int[CRC_LOOKUP_SIZE];

        for (int index = 0; index < CRC_LOOKUP_SIZE; index++) {
            int val = index << 24;

            for(int i = 0; i < 8; i++){
                if ((val & 0x80000000) != 0){
                    val = (val << 1)^0x04c11db7;
                } else {
                    val <<= 1;
                }
            }

            table[index] = val;
        }

        return table;
    }

    public CRC() {
        reset();
    }

    @Override
    public void update(int b) {
        update(new byte[]{(byte)b}, 0, 1);
    }

    @Override
    public void update(byte[] b, int off, int len) {
        for (int i = off; i < (off + len); i++) {
            //noinspection MagicNumber
            ret = (ret << 8) ^ CRC_LOOKUP[((ret >>> 24)& 0xFF) ^ (b[i] & 0xFF)];
        }
    }

    @Override
    public long getValue() {
        //noinspection MagicNumber
        return ret & 0xFFFFFFFFL;
    }

    @Override
    public void reset() {
        ret = 0;
    }
}
