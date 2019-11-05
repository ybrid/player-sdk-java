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

import io.ybrid.api.Session;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

/**
 * This factory is used to build {@link DataSource DataSources}.
 */
public class DataSourceFactory {
    private static class URLSource implements ByteDataSource {
        private InputStream inputStream;
        private String contentType;

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
            return new ByteDataBlock(null, inputStream, 1024*2);
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

    /**
     * This builds a {@link ByteDataSource} for the audio stream of a {@link Session}.
     *
     * @param session The {@link Session} to use.
     * @return The {@link ByteDataSource} for the stream.
     * @throws IOException I/O-Errors as thrown by the used backends.
     */
    public static ByteDataSource getSourceBySession(Session session) throws IOException {
        URL url = session.getStreamURL();

        try {
            return new ICYInputStream(url);
        } catch (MalformedURLException ignored) {
        }

        return new URLSource(url);
    }
}
