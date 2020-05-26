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

import io.ybrid.api.Session;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory is used to build {@link DataSource DataSources}.
 */
public final class DataSourceFactory {
    static final Logger LOGGER = Logger.getLogger(DataSourceFactory.class.getName());

    private static class URLSource implements ByteDataSource {
        private final InputStream inputStream;
        private final String contentType;

        private static void acceptListToHeader(@NotNull URLConnection connection, @NonNls @NotNull String header, @Nullable Map<String, Double> list) {
            @NonNls StringBuilder ret = new StringBuilder();

            if (list == null || list.isEmpty())
                return;

            for (Map.Entry<String, Double> entry : list.entrySet()) {
                if (ret.length() > 0)
                    ret.append(", ");
                ret.append(entry.getKey()).append("; q=").append(entry.getValue());
            }

            connection.setRequestProperty(header, ret.toString());
        }

        public URLSource(Session session) throws IOException {
            @NonNls URLConnection connection = session.getStreamURL().openConnection();

            connection.setDoInput(true);
            connection.setDoOutput(false);

            acceptListToHeader(connection, HttpHelper.HEADER_ACCEPT, session.getAcceptedMediaFormats());
            acceptListToHeader(connection, HttpHelper.HEADER_ACCEPT_LANGUAGE, session.getAcceptedLanguages());
            connection.setRequestProperty("Accept-Charset", "utf-8, *; q=0");

            connection.connect();

            inputStream = connection.getInputStream();
            contentType = connection.getContentType();
        }

        @Override
        public @NotNull ByteDataBlock read() throws IOException {
            //noinspection MagicNumber
            return new ByteDataBlock(null, null, inputStream, 1024*2);
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

        LOGGER.log(Level.INFO, "getSourceBySession(session="+session+"): url=" + url); //NON-NLS

        try {
            return new ICYInputStream(session);
        } catch (MalformedURLException ignored) {
        }

        return new URLSource(session);
    }
}
