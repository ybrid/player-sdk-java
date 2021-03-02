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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This class represents a single Ogg page as per RFC 3533.
 */
public final class Page implements hasGranularPosition {
    private static final int READ_REQUEST = 4096; // [B], request reading 4kB at once.
    private static final int MIN_OGG_HEADER_LENGTH = 27; // [B]
    private static final byte[] MAGIC = new byte[]{'O', 'g', 'g', 'S', 0};

    private final @NotNull OggVersion version;
    private final @NotNull EnumSet<Flag> flags;
    private final @NotNull GranularPosition granularPosition;
    private final int serial;
    private final int sequence;
    private final @NotNull byte[] crc;
    private final int segments;
    private final @NotNull byte[] segmentTable;
    private final @NotNull byte[] body;

    private static boolean arrayBeginsWith(@NotNull byte[] haystack, int offset, @NotNull byte[] needle) {
        if ((haystack.length - offset) < needle.length)
            return false;

        for (int i = 0; i < needle.length; i++) {
            if (haystack[offset + i] != needle[i])
                return false;
        }

        return true;
    }

    @SuppressWarnings("MagicNumber")
    private static boolean checkCRC(@NotNull byte[] raw, int offset, int length) {
        final @NotNull CRC crc = new CRC();
        final @NotNull byte[] data = Util.extractBytes(raw, offset, length);

        crc.update(raw, offset, 22);
        crc.update(new byte[4], 0, 4);
        crc.update(raw, offset + 26, length - 26);
        return (int)crc.getValue() == Util.readLE32(raw, offset + 22);
    }

    @SuppressWarnings("MagicNumber")
    private static @Nullable SyncRequest verifyInner(@NotNull byte[] raw, int offset) {
        int headerLength = MIN_OGG_HEADER_LENGTH;
        int bodyLength = 0;
        int segments;

        if ((raw.length - offset) < MIN_OGG_HEADER_LENGTH)
            return new SyncRequest(offset, READ_REQUEST, null);

        segments = raw[offset + 26] & 0xFF;
        headerLength += segments;

        if ((raw.length - offset) < headerLength)
            return new SyncRequest(offset, READ_REQUEST, null);

        for (int i = 0; i < segments; i++) {
            bodyLength += raw[offset + 27 + i] & 0xFF;
        }

        if ((raw.length - offset) < (headerLength + bodyLength))
            return new SyncRequest(offset, (headerLength + bodyLength) - (raw.length - offset), null);

        if (!checkCRC(raw, offset, headerLength + bodyLength))
            return null;

        return new SyncRequest(offset, 0, headerLength + bodyLength);
    }

    public static @NotNull SyncRequest verify(@NotNull byte[] raw, int offset) {
        int i;

        // if we have less than a MAGIC's length left request new data right away!
        if ((raw.length - offset) < MAGIC.length)
            return new SyncRequest(raw.length, READ_REQUEST, null);

        for (i = offset; i < (raw.length - MAGIC.length); i++) {
            if (arrayBeginsWith(raw, i, MAGIC)) {
                final @Nullable SyncRequest request = verifyInner(raw, i);
                if (request != null)
                    return request;
            }
        }

        // Nothing found, but keep MAGIC's length bytes to ensure we do not miss a partial MAGIC.
        return new SyncRequest(raw.length - MAGIC.length, READ_REQUEST, null);
    }

    /**
     * Constructs a page object from a array of raw bytes.
     * @param raw The bytes to read from.
     * @param offset The offset to use.
     */
    @SuppressWarnings("MagicNumber")
    public Page(@NotNull byte[] raw, int offset) {
        final @NotNull SyncRequest request = verify(raw, offset);
        int bodyLength = 0;

        if (request.getSkip() != offset)
            throw new IllegalArgumentException("Offset invalid.");
        if (request.getValid() == null)
            throw new IllegalArgumentException("No valid page in raw array.");

        this.version = OggVersion.valueOf(raw[offset + 4]);
        this.flags = Flag.valuesOf(raw[offset + 5]);
        granularPosition = new GranularPosition(raw, offset + 6);
        serial = Util.readLE32(raw, offset + 14);
        sequence = Util.readLE32(raw, offset + 18);
        crc = Util.extractBytes(raw, offset + 22, 4);
        segments = raw[offset + 26] & 0xFF;
        segmentTable = new byte[segments];
        System.arraycopy(raw, offset + 27, segmentTable, 0, segments);
        for (int i = 0; i < segments; i++) {
            bodyLength += raw[offset + 27 + i] & 0xFF;
        }
        body = Util.extractBytes(raw, offset + 27 + segments, bodyLength);
    }

    /**
     * Gets the {@link OggVersion} of the current page.
     * @return The version of the current page.
     */
    public @NotNull OggVersion getVersion() {
        return version;
    }

    /**
     * Gets the {@link Flag flags} set on this page.
     * @return The set flags.
     */
    public @NotNull @UnmodifiableView Set<Flag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    @Override
    public @NotNull GranularPosition getGranularPosition() {
        return granularPosition;
    }

    /**
     * Gets the serial number of the bitstream from the page.
     * @return The serial number.
     */
    @Contract(pure = true)
    public int getSerial() {
        return serial;
    }

    /**
     * Gets the sequence number of the page.
     * @return The sequence number.
     */
    @Contract(pure = true)
    public int getSequence() {
        return sequence;
    }

    /**
     * Gets the number of segments from the page.
     *
     * This is generally not very useful. {@link Stream} should be used
     * to extract {@link Packet packets} from pages.
     *
     * @return The number of segments in this page.
     */
    @Contract(pure = true)
    public int getSegments() {
        return segments;
    }

    /**
     * Gets the raw segment table.
     *
     * This is generally not very useful. {@link Stream} should be used
     * to extract {@link Packet packets} from pages.
     *
     * @return The raw segment table.
     */
    @Contract(pure = true)
    public byte[] getSegmentTable() {
        return segmentTable;
    }

    /**
     * Gets the raw body from the page.
     *
     * This is generally not very useful. {@link Stream} should be used
     * to extract {@link Packet packets} from pages.
     *
     * @return The raw body.
     */
    @Contract(pure = true)
    public byte[] getBody() {
        return body;
    }

    /**
     * Finds an array of bytes within the page.
     * This is only useful for checking a {@link Flag#BOS} page
     * for a given magic in order to select a mapping.
     *
     * @param offset The offset in the body to look for the needle at.
     * @param needle The needle to look for.
     * @return Whether the needle is found at the offset within the body of the page.
     */
    public boolean bodyContains(int offset, @NotNull byte[] needle) {
        return arrayBeginsWith(body, offset, needle);
    }

    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    public String toString() {
        return "Page{" +
                "version=" + version +
                ", flags=" + flags +
                ", granularPosition=" + granularPosition +
                ", serial=" + serial +
                ", sequence=" + sequence +
                ", crc=" + Arrays.toString(crc) +
                ", segments=" + segments +
                ", segmentTable=" + Arrays.toString(segmentTable) +
                ", body=" + (body[0] == 'O' ? new String(body, StandardCharsets.UTF_8) : "<binary>") +
                "}";
    }
}
