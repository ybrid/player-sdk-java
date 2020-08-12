/*
 * Copyright (c) 2019 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

import io.ybrid.api.metadata.Metadata;
import io.ybrid.api.PlayoutInfo;

/**
 * This class represents generic blocks of data. Instances also store {@link Metadata} for each block.
 */
public class DataBlock {
    /**
     * Internal storage for the {@link Metadata} hold by the object.
     */
    protected Metadata metadata;
    /**
     * Internal storage for the {@link PlayoutInfo} hold by the object.
     */
    protected PlayoutInfo playoutInfo;

    /**
     * Create a new DataBlock.
     *
     * @param metadata The {@link Metadata} to use for the new DataBlock.
     */
    protected DataBlock(Metadata metadata, PlayoutInfo playoutInfo) {
        this.metadata = metadata;
        this.playoutInfo = playoutInfo;
    }

    /**
     * Gets the {@link Metadata} of the block.
     *
     * @return The {@link Metadata} of the block.
     */
    public Metadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the {@link Metadata} of the block.
     * @param metadata The {@link Metadata} to set.
     */
    public void setMetadata(Metadata metadata) {
        this.metadata = metadata;
    }

    /**
     * Gets the {@link PlayoutInfo} of the block.
     *
     * @return The {@link PlayoutInfo} of the block.
     */
    public PlayoutInfo getPlayoutInfo() {
        return playoutInfo;
    }

    /**
     * Sets the {@link PlayoutInfo} of the block.
     * @param playoutInfo The {@link PlayoutInfo} to set.
     */
    public void setPlayoutInfo(PlayoutInfo playoutInfo) {
        this.playoutInfo = playoutInfo;
    }
}
