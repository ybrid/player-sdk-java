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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class implements a buffer allowing to read Ogg {@link Page pages} from
 * a data source providing raw bytes.
 */
public final class Sync {
    private static final int MAX_RETRY = 3;

    private @NotNull  byte[] buffer = new byte[0];
    private int bufferOffset = 0;
    private @Nullable InputStream autoFullSource = null;
    private boolean eofOnAutoFill = false;

    /**
     * Fills the buffer with additional data by appending it at the end.
     *
     * @param raw The source array to take bytes from.
     * @param offset The offset in the source array to start reading bytes from.
     * @param length The length to read starting at the given offset.
     * @see #setAutoFillSource(InputStream)
     */
    public void fill(byte[] raw, int offset, int length) {
        final @NotNull byte[] n = new byte[buffer.length - bufferOffset + length];
        System.arraycopy(buffer, bufferOffset, n, 0, buffer.length - bufferOffset);
        System.arraycopy(raw, offset, n, buffer.length - bufferOffset, length);
        buffer = n;
        bufferOffset = 0;
    }

    private boolean canAutoFill() {
        return autoFullSource != null && !eofOnAutoFill;
    }

    private void autoFill(int length) throws IOException {
        if (canAutoFill()) {
            final @NotNull byte[] n = new byte[length];
            try {
                int ret = autoFullSource.read(n);
                if (ret < 0) {
                    eofOnAutoFill = true;
                } else if (ret > 0) {
                    fill(n, 0, ret);
                }
            } catch (EOFException e) {
                eofOnAutoFill = true;
            }
        }
    }

    private void handle(@NotNull SyncRequest request) throws IOException {
        bufferOffset = request.getSkip();
        autoFill(request.getRead());
    }

    /**
     * Returns whether the auto fill source signaled EOF.
     * @return The EOF state of the autofill source.
     * @see #setAutoFillSource(InputStream)
     */
    public boolean isEofOnAutoFill() {
        return eofOnAutoFill;
    }

    /**
     * Sets a data source for auto filling.
     * In auto filling mode calls to {@link #read()} will automatically read from this source
     * and append the data to the internal buffer as if {@link #fill(byte[], int, int)} was called
     * to fill the buffer when the request for a page can not be satisfied with the current buffer.
     * <P>
     * It is uncommon but valid to set a new source at any time including after EOF has been reached and
     * signaled by {@link #isEofOnAutoFill()}. The EOF flag is reset by calling this method.
     *
     * @param autoFullSource The source to set.
     * @see #fill(byte[], int, int)
     * @see #isEofOnAutoFill()
     */
    public void setAutoFillSource(@Nullable InputStream autoFullSource) {
        this.autoFullSource = autoFullSource;
        this.eofOnAutoFill = false;
    }

    /**
     * Reads a {@link Page} from the buffer. If no {@link Page} can be read {@code null} is returned.
     * This may read from auto fill sources if any are set.
     * @return The newly parsed {@link Page} or {@code null}.
     * @throws IOException As thrown by reading from backends.
     */
    public @Nullable Page read() throws IOException {
        for (int retry = 0; retry < MAX_RETRY; retry++) {
            final @NotNull SyncRequest request = Page.verify(buffer, bufferOffset);
            if (request.getValid() != null) {
                final @NotNull Page page = new Page(buffer, bufferOffset);
                bufferOffset = request.getSkip() + request.getValid();
                return page;
            }
            if (!canAutoFill())
                break;
            handle(request);
        }
        return null;
    }
}
