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

package io.ybrid.player.io;

import io.ybrid.api.Metadata;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class ByteDataBlock extends DataBlock {
    protected byte[] data;

    public ByteDataBlock(Metadata metadata, byte[] data) {
        super(metadata);
        this.data = data;
    }

    public ByteDataBlock(Metadata metadata, InputStream inputStream, int length) throws IOException {
        super(metadata);
        data = new byte[1024];
        int ret = inputStream.read(data);
        if (ret < 1)
            throw new EOFException();

        if (ret != data.length) {
            byte[] newBuffer = new byte[ret];
            System.arraycopy(data, 0, newBuffer, 0, ret);
            data = newBuffer;
        }
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        return "ByteDataBlock{" +
                "data.length=" + data.length +
                ", metadata=" + metadata +
                '}';
    }
}
