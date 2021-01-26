/*
 * Copyright (c) 2021 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

package io.ybrid.player.io;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This is the base class for all classes that implement filters for {@link DataSource}es.
 */
public class FilterDataSource<T extends DataSource> implements DataSource {
    protected final @NotNull T backend;

    /**
     * The main constructor.
     * @param backend The backend to use.
     */
    @Contract(pure = true)
    public FilterDataSource(@NotNull T backend) {
        this.backend = backend;
    }

    @Override
    @NotNull
    public DataBlock read() throws IOException {
        return backend.read();
    }

    @Override
    public void mark() {
        backend.mark();
    }

    @Override
    public void reset() {
        backend.reset();
    }

    @Override
    public boolean isValid() {
        return backend.isValid();
    }

    @Override
    @Nullable
    public String getContentType() {
        return backend.getContentType();
    }

    @Override
    public void close() throws IOException {
        backend.close();
    }
}
