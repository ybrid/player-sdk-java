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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

public class MetadataInputStream extends InputStream {
    private ByteDataSource source;
    private Metadata metadata;
    private Metadata nextMetadata;
    private byte[] buffer;
    private int offset;

    public MetadataInputStream(ByteDataSource source) {
        this.source = source;
    }

    public Metadata getMetadata() throws IOException {
        fillBuffer();
        if (metadata == null)
            metadata = nextMetadata;
        return metadata;
    }

    @Override
    public int read() throws IOException {
        byte[] buf = new byte[1];
        int ret = read(buf, 0, 1);
        if (ret != 1)
            return -1;
        return buf[0];
    }

    private void fillBuffer() throws IOException {
        ByteDataBlock block;

        if (buffer != null && buffer.length == offset) {
            buffer = null;
            offset = 0;
        }

        if (buffer != null)
            return;

        try {
            block = source.read();

            buffer = block.getData();
            nextMetadata = block.getMetadata();
        } catch (EOFException ignored) {
            buffer = null;
            nextMetadata = null;
        }
    }

    private int pump(byte[] b, int off, int len) throws IOException {
        int todo;

        fillBuffer();

        if (buffer == null)
            return 0;

        todo = buffer.length - offset;
        if (todo > len)
            todo = len;

        System.arraycopy(buffer, offset, b, off, todo);
        offset += todo;

        fillBuffer();

        return todo;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int ret = 0;
        int res;

        while (len > 0) {
            res = pump(b, off, len);
            if (res <= 0)
                break;

            off += res;
            len -= res;
            ret += res;
        }

        if (nextMetadata != null)
            metadata = nextMetadata;

        return ret;
    }
}
