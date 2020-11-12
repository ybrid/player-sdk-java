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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class Util {
    /**
     * Read a 64 bit little endian value as long.
     * @param raw Buffer with input data.
     * @param offset Offset of the value within the buffer.
     * @return The value in native byte order.
     */
    @Contract(pure = true)
    @SuppressWarnings("MagicNumber")
    public static long readLE64(@NotNull byte[] raw, int offset) {
        long ret = 0;
        for (int i = 0; i < 8; i++) {
            ret += (raw[offset + i] & 0xFF) << (8 * i);
        }
        return ret;
    }

    /**
     * Read a 32 bit little endian value as int.
     * @param raw Buffer with input data.
     * @param offset Offset of the value within the buffer.
     * @return The value in native byte order.
     */
    @Contract(pure = true)
    @SuppressWarnings("MagicNumber")
    public static int readLE32(@NotNull byte[] raw, int offset) {
        int ret = 0;
        for (int i = 0; i < 4; i++) {
            ret += (raw[offset + i] & 0xFF) << (8 * i);
        }
        return ret;
    }

    /**
     * Extracts a subarray.
     * @param raw The base array.
     * @param offset The offset to start extracting from.
     * @param length The length of the subarray to extract.
     * @return The extracted subarray.
     */
    @Contract(pure = true)
    public static @NotNull byte[] extractBytes(@NotNull byte[] raw, int offset, int length) {
        final @NotNull byte[] ret = new byte[length];
        System.arraycopy(raw, offset, ret, 0, length);
        return ret;
    }

    /**
     * Concatenate a array with a subarray.
     * @param base The base array to append to.
     * @param addition The array to take the additional elements from.
     * @param offset The offset of the addition array.
     * @param length The length of the addition.
     * @return The newly constructed array.
     */
    @Contract(pure = true)
    public static @NotNull byte[] appendSubArray(@NotNull byte[] base, @NotNull byte[] addition, int offset, int length) {
        final @NotNull byte[] n = new byte[base.length + length];
        System.arraycopy(base, 0, n, 0, base.length);
        System.arraycopy(addition, offset, n, base.length, length);
        return n;
    }
}
