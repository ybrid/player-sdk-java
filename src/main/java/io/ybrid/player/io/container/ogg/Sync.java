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

public class Sync {
    private static final int MAX_RETRY = 3;

    private @NotNull  byte[] buffer = new byte[0];
    private int bufferOffset = 0;
    private @Nullable InputStream autoFullSource = null;
    private boolean eofOnAutoFill = false;

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

    public boolean isEofOnAutoFill() {
        return eofOnAutoFill;
    }

    public void setAutoFullSource(@Nullable InputStream autoFullSource) {
        this.autoFullSource = autoFullSource;
        this.eofOnAutoFill = false;
    }

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
