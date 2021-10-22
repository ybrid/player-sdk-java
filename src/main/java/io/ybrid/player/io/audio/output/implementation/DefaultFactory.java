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

package io.ybrid.player.io.audio.output.implementation;

import io.ybrid.player.io.audio.output.AudioOutput;
import io.ybrid.player.io.audio.output.AudioOutputFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

@ApiStatus.Internal
public final class DefaultFactory implements AudioOutputFactory {
    private final static Factory[] drivers = new Factory[]{
            new Factory(Javax.class),
            new Factory(LazyLoadingAndroidAudioOutput.class)
    };

    public final static AudioOutputFactory INSTANCE = new DefaultFactory();

    @Contract(pure = true)
    private DefaultFactory() {
    }

    private static final class Factory {
        private final Class<? extends Base> supplier;

        @Contract(pure = true)
        public Factory(Class<? extends Base> supplier) {
            this.supplier = supplier;
        }

        public @NotNull AudioOutput tryGetDriver() throws Throwable {
            final @NotNull Base driver = supplier.newInstance();

            if (!driver.available())
                throw new UnsupportedOperationException();

            return driver;
        }
    }

    @Override
    public @NotNull AudioOutput getAudioOutput() {
        for (final @NotNull Factory factory : drivers) {
            try {
                return factory.tryGetDriver();
            } catch (Throwable ignored) {
            }
        }

        throw new UnsupportedOperationException();
    }
}
