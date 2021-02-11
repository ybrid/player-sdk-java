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

package io.ybrid.player.io.audio.generator;

import org.jetbrains.annotations.NotNull;

public abstract class Function {
    public interface State {
    }

    abstract @NotNull State createState();

    abstract short[] generate(@NotNull State state, int sampleRate, int channels, int frames);

    static public @NotNull Function createSilence() {
        return new Function() {
            @Override
            @NotNull State createState() {
                //noinspection InnerClassTooDeeplyNested
                return new State() {};
            }

            @Override
            short[] generate(@NotNull State state, int sampleRate, int channels, int frames) {
                return new short[channels*frames];
            }
        };
    }

    static public @NotNull Function createCos(double frequency, double amplitude, double initialPhase) {
        return new Function() {
            @SuppressWarnings("InnerClassTooDeeplyNested")
            class CosState implements State {
                private double phase = initialPhase;

                public double getPhaseAndAdvance(int sampleRate) {
                    final double oldPhase = phase;

                    while (phase > Math.PI)
                        phase -= 2.*Math.PI;

                    phase += 2.*Math.PI * frequency / (double)sampleRate;

                    return oldPhase;
                }
            }
            @Override
            @NotNull State createState() {
                return new CosState();
            }

            @Override
            short[] generate(@NotNull State state, int sampleRate, int channels, int frames) {
                short[] out = new short[channels*frames];

                for (int i = 0; i < frames; i++) {
                    short value = (short)(Short.MAX_VALUE * amplitude * Math.cos(((CosState)state).getPhaseAndAdvance(sampleRate)));

                    for (int c = 0; c < channels; c++) {
                        out[i * channels + c] = value;
                    }
                }

                return out;
            }
        };
    }
}
