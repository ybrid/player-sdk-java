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
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.LinkedList;
import java.util.Queue;

public class Stream {
    private final @NotNull Queue<Packet> readyPackets = new LinkedList<>();
    private final @NotNull EnumSet<Flag> seenFlags = EnumSet.noneOf(Flag.class);
    private final int serial;
    private int lastSequence;
    private final @NotNull EnumSet<Flag> bufferedFlags = EnumSet.noneOf(Flag.class);
    private byte[] bufferedBody;

    private void assertPageIsValid(@NotNull Page page) {
        if (page.getSerial() != serial)
            throw new IllegalArgumentException("Page for wrong bitstream, serial expected: " + serial + ", but got: " + page.getSerial());

        if (page.getFlags().contains(Flag.BOS)) {
            if (seenFlags.contains(Flag.BOS))
                throw new IllegalArgumentException("Page with BOS set on a already open stream.");
            seenFlags.add(Flag.BOS);
        }

        if (page.getFlags().contains(Flag.EOS)) {
            if (seenFlags.contains(Flag.EOS))
                throw new IllegalArgumentException("Page with EOS set on a already closed stream.");
            seenFlags.add(Flag.EOS);
        }
    }

    private void clearBuffer() {
        bufferedBody = null;
        bufferedFlags.clear();
    }

    private void handleSegment(@NotNull byte[] raw, int offset, int length, boolean afterHole, boolean continued, boolean toBeContinued, int segment, @NotNull Page page) {
        final @NotNull GranularPosition granularPosition;

        if (afterHole) {
            clearBuffer();
        } else {
            if ((bufferedBody == null) == continued)
                throw new IllegalArgumentException("Continued page after non-continued segment with no hole");
        }

        if (bufferedBody != null) {
            bufferedBody = Util.appendSubArray(bufferedBody, raw, offset, length);
        } else {
            bufferedBody = Util.extractBytes(raw, offset, length);
        }

        if (segment == 0 && page.getFlags().contains(Flag.BOS))
            bufferedFlags.add(Flag.BOS);

        if (segment == (page.getSegments() - 1)) {
            if (page.getFlags().contains(Flag.EOS))
                bufferedFlags.add(Flag.EOS);
            granularPosition = page.getGranularPosition();
        } else {
            granularPosition = GranularPosition.INVALID;
        }

        if (!toBeContinued) {
            readyPackets.add(new Packet(granularPosition, bufferedFlags, bufferedBody, afterHole));
            clearBuffer();
        }
    }

    private void extractSegements(@NotNull Page page, boolean afterHole) {
        final @NotNull byte[] body = page.getBody();
        final @NotNull byte[] segmentTable = page.getSegmentTable();
        boolean continued = page.getFlags().contains(Flag.CONTINUED);
        int segmentTotalLength = 0;
        int segmentBodyOffset = 0;

        for (int segment = 0; segment < page.getSegments(); segment++) {
            //noinspection MagicNumber
            int length = segmentTable[segment] & 0xFF;

            segmentTotalLength += length;

            //noinspection MagicNumber
            if (length == 255) {
                continue;
            }

            handleSegment(body, segmentBodyOffset, segmentTotalLength, afterHole, continued, false, segment, page);
            afterHole = false;
            continued = false;
            segmentBodyOffset += segmentTotalLength;
            segmentTotalLength = 0;
        }

        if (segmentTotalLength != 0)
            handleSegment(body, segmentBodyOffset, segmentTotalLength, afterHole, continued, true, page.getSegments() - 1, page);
    }

    public Stream(@NotNull Page page) {
        if (!page.getFlags().contains(Flag.BOS))
            throw new IllegalArgumentException("Stream does not begin with BOS page");
        serial = page.getSerial();
        lastSequence = page.getSequence() - 1;
        add(page);
    }

    public void add(@NotNull Page page) {
        final boolean hole;

        assertPageIsValid(page);

        hole = page.getSequence() != (lastSequence + 1);
        lastSequence = page.getSequence();

        extractSegements(page, hole);
    }

    public @Nullable Packet read() {
        return readyPackets.poll();
    }
}
