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

package io.ybrid.player.io.muxer;

import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.ByteDataSource;
import io.ybrid.player.io.DataBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class Demuxer<T extends Stream<T, ?, ?, I>, I extends DataBlock> {
    protected boolean eofOnAutofill = false;
    protected @Nullable ByteDataSource autofillSource;
    protected @Nullable Predicate<@NotNull DataBlock> isWantedCallback;
    protected @Nullable Consumer<@NotNull T> onBeginOfStreamCallback;
    protected @Nullable Consumer<@NotNull T> onEndOfStreamCallback;

    protected <K> void runConsumer(@Nullable Consumer<@NotNull K> consumer, @NotNull K argument) {
        if (consumer == null)
            return;

        try {
            consumer.accept(argument);
        } catch (Throwable ignored) {
        }
    }

    protected <K> boolean runPredicate(@Nullable Predicate<@NotNull K> predicate, @NotNull K argument, boolean def) {
        if (predicate == null)
            return def;

        try {
            return predicate.test(argument);
        } catch (Throwable e) {
            return def;
        }
    }

    public void setIsWantedCallback(@Nullable Predicate<DataBlock> isWantedCallback) {
        this.isWantedCallback = isWantedCallback;
    }

    public void setOnBeginOfStreamCallback(@Nullable Consumer<T> onBeginOfStreamCallback) {
        this.onBeginOfStreamCallback = onBeginOfStreamCallback;
    }

    public void setOnEndOfStreamCallback(@Nullable Consumer<T> onEndOfStreamCallback) {
        this.onEndOfStreamCallback = onEndOfStreamCallback;
    }

    public void setAutofillSource(@Nullable ByteDataSource autofillSource) {
        this.autofillSource = autofillSource;
        this.eofOnAutofill = false;
    }

    protected void autofill() throws IOException {
        if (autofillSource == null || eofOnAutofill)
            return;

        try {
            fill(autofillSource.read());
        } catch (EOFException e) {
            eofOnAutofill = true;
        }
    }

    public boolean isEofOnAutofill() {
        return eofOnAutofill;
    }

    public boolean canAutofill() {
        return autofillSource != null;
    }

    public abstract void fill(@NotNull ByteDataBlock block);
    public abstract void iter() throws IOException;
}
