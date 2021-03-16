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

import io.ybrid.api.util.MediaType;
import io.ybrid.player.io.DataSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * This interface is implemented by classes that allow reading PCM data as {@link PCMDataBlock}.
 */
public interface PCMDataSource extends DataSource {
    /**
     * Read a {@link PCMDataBlock}.
     *
     * @return The block that has been read.
     * @throws IOException And I/O-Errors occurred while reading the block.
     */
    @Override
    @NotNull PCMDataBlock read() throws IOException;

    /**
     * Gets the number of samples skipped by this data source including all backends.
     * <P>
     * For filter sources (sources using another source as backend) this should
     * return the number of samples skipped by this source plus the number of samples skipped by any backend.
     * As each backend can skip samples at any point implementations must not cache values from their backends.
     * <P>
     * For non-filter streams this should return zero.
     * <P>
     * Note: Samples that are skipped by decoders on behalf of the decoded format such as Ogg skips or Opus pre-skip
     *       must not be included in this value.
     * The default implementation returns zero.
     *
     * @return Returns the number of skipped samples.
     */
    default long getSkippedSamples() {
        return 0;
    }

    @Override
    @Nullable
    default MediaType getMediaType() {
        return new MediaType(io.ybrid.player.io.MediaType.PCM_STREAM_SHORT);
    }
}
