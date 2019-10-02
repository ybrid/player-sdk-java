/*
 * Copyright 2019 nacamar GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.ybrid.client.player.io;

import io.ybrid.client.control.Metadata;

public class PCMDataBlock extends DataBlock {
    protected short[] data;
    protected int sampleRate;
    protected int numberOfChannels;

    public PCMDataBlock(Metadata metadata, short[] data, int sampleRate, int numberOfChannels) {
        super(metadata);
        this.data = data;
        this.sampleRate = sampleRate;
        this.numberOfChannels = numberOfChannels;
    }

    public short[] getData() {
        return data;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getNumberOfChannels() {
        return numberOfChannels;
    }

    public double getBlockLength() {
        return (double)getData().length / (double)(getSampleRate() * getNumberOfChannels());
    }
}
