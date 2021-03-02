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

package io.ybrid.player.io.audio;

import io.ybrid.api.metadata.Sync;
import io.ybrid.api.metadata.source.Source;
import io.ybrid.api.metadata.source.SourceType;
import io.ybrid.player.io.audio.analysis.result.Block;
import io.ybrid.player.io.audio.analysis.result.Channel;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PCMDataBlockTest {
    private static final int SAMPLE_RATE = 48000;
    private static final int CHANNELS = 2;
    private static final short[] data = new short[]{0, 1,  2, 3,  4, 5,  6, 7};

    private PCMDataBlock block;

    static void expectFail(@NotNull Class<? extends Throwable> expected, @NotNull Runnable runnable) {
        try {
            runnable.run();
        } catch (Throwable e) {
            if (!expected.isInstance(e)) {
                assertEquals(expected, e.getClass());
            }

            return;
        }

        fail("Call did not fail. Expected: " + expected.getName());
    }

    @Before
    public void setUp() {
        final @NotNull Sync sync = Sync.Builder.buildEmpty(new Source(SourceType.SESSION));
        block = new PCMDataBlock(sync, null, data, SAMPLE_RATE, CHANNELS);
    }

    @Test
    public void getSampleRate() {
        assertEquals(SAMPLE_RATE, block.getSampleRate());
    }

    @Test
    public void getNumberOfChannels() {
        assertEquals(CHANNELS, block.getNumberOfChannels());
    }

    @Test
    public void getLengthAsFrames() {
        assertEquals(data.length/CHANNELS, block.getLengthAsFrames());
    }

    @Test
    public void analyse() {
        final @NotNull Block res = block.analyse();
        final @NotNull Channel[] channels = res.getChannels();

        assertEquals(CHANNELS, channels.length);
        assertEquals(data[0], channels[0].getMinAsShort());
        assertEquals(data[1], channels[1].getMinAsShort());
        assertEquals(data[6], channels[0].getMaxAsShort());
        assertEquals(data[7], channels[1].getMaxAsShort());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void subBlock() {
        final @NotNull PCMDataBlock n = block.subBlock(2, 3);
        final @NotNull Block res = n.analyse();
        final @NotNull Channel[] channels = res.getChannels();
        System.out.println("n.analyse() = " + res);

        assertEquals(block.getLengthAsFrames() - 3, n.getLengthAsFrames());
        assertEquals(data[4], channels[0].getMinAsShort());
        assertEquals(data[5], channels[1].getMinAsShort());
        assertEquals(data[4], channels[0].getMaxAsShort());
        assertEquals(data[5], channels[1].getMaxAsShort());

        expectFail(IllegalArgumentException.class, () -> block.subBlock(-1, -1));
        expectFail(IllegalArgumentException.class, () -> block.subBlock(-1, 0));
        expectFail(IllegalArgumentException.class, () -> block.subBlock(0, -1));
        expectFail(IllegalArgumentException.class, () -> block.subBlock(2, 1));
    }

    @Test
    public void trim() {
        final @NotNull PCMDataBlock n = block.trim(1, 1);
        final @NotNull Block res = n.analyse();
        final @NotNull Channel[] channels = res.getChannels();
        System.out.println("n.analyse() = " + res);

        assertEquals(block.getLengthAsFrames() - 2, n.getLengthAsFrames());
        assertEquals(data[2], channels[0].getMinAsShort());
        assertEquals(data[3], channels[1].getMinAsShort());
        assertEquals(data[4], channels[0].getMaxAsShort());
        assertEquals(data[5], channels[1].getMaxAsShort());
    }
}