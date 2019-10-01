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
import io.ybrid.client.control.Session;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class DataSourceFactory {
    private static class URLSource implements ByteDataSource {
        private InputStream inputStream;
        private String contentType;

        private static class Block implements ByteDataBlock {
            private byte[] buffer;

            Block(InputStream inputStream) throws IOException {
                buffer = new byte[1024];
                int ret = inputStream.read(buffer);
                if (ret < 1)
                    throw new EOFException();

                if (ret != buffer.length) {
                    byte[] newBuffer = new byte[ret];
                    System.arraycopy(buffer, 0, newBuffer, 0, ret);
                    buffer = newBuffer;
                }
            }

            @Override
            public byte[] getData() {
                return buffer;
            }

            @Override
            public Metadata getMetadata() {
                return null;
            }
        }

        public URLSource(URL url) throws IOException {
            URLConnection connection = url.openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(false);
            connection.connect();

            inputStream = connection.getInputStream();
            contentType = connection.getContentType();
        }

        @Override
        public ByteDataBlock read() throws IOException {
            return new Block(inputStream);
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public void close() throws IOException {
            inputStream.close();
        }
    }
    public static ByteDataSource getSourceBySession(Session session) throws IOException {
        // TODO.
        return new URLSource(session.getStreamURL());
    }
}
