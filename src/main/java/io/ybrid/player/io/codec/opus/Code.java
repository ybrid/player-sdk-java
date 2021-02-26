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

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Frame packing code as defined by RFC 6716 Section 3.2. See also Section 3.
 */
public enum Code {
    /**
     * Code 0: One Frame is encoded.
     */
    ONE_FRAME(0),
    /**
     * Code 1: Two equal sized frames are encoded.
     */
    TWO_EQUAL_SIZED_FRAMES(1),
    /**
     * Code 2: Two frames are encoded each of a different size.
     */
    TWO_DIFFERENT_SIZED_FRAMES(2),
    /**
     * Code 3: A arbitrary number of frames are encoded.
     */
    ARBITRARY_NUMBER_OF_FRAMES(3);

    private static final @NotNull Map<Integer, Code> values = new HashMap<>();

    private final int number;

    static {
        for (final @NotNull Code code : values())
            values.put(code.number, code);
    }

    /**
     * Gets the Code based on it's number. See RFC 6716 Section 3.2 for valid numbers.
     * @param val The code number.
     * @return The corresponding code.
     */
    public static @NotNull Code valueOf(int val) {
        return values.get(val);
    }

    Code(int number) {
        this.number = number;
    }
}
