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

package io.ybrid.player.io.protocol;

import io.ybrid.api.TemporalValidity;
import io.ybrid.api.bouquet.source.ICEBasedService;
import io.ybrid.api.message.MessageBody;
import io.ybrid.api.metadata.Sync;
import io.ybrid.api.transport.ServiceURITransportDescription;
import io.ybrid.api.transport.TransportConnectionState;
import io.ybrid.player.io.ByteDataBlock;
import io.ybrid.player.io.ByteDataSource;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

@ApiStatus.Internal
public class URLSource implements ByteDataSource {
    private final @NotNull ServiceURITransportDescription transportDescription;
    private final @NotNull Sync sync;
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

    private @NotNull Map<@NotNull String, @NotNull String> getHeadersAsMap(@NotNull URLConnection connection) {
        final @NotNull Map<@NotNull String, @NotNull String> ret = new HashMap<>();

        for (int i = 0; ; i++) {
            final @Nullable String key = connection.getHeaderFieldKey(i);
            final @Nullable String value = connection.getHeaderField(i);
            if (key == null || value == null)
                break;
            ret.put(key, value);
        }

        return ret;
    }

    public URLSource(@NotNull ServiceURITransportDescription transportDescription) throws IOException {
        @NonNls URLConnection connection = transportDescription.getURI().toURL().openConnection();
        final @Nullable MessageBody messageBody = transportDescription.getRequestBody();

        this.transportDescription = transportDescription;

        transportDescription.signalConnectionState(TransportConnectionState.CONNECTING);

        try {
            connection.setDoInput(true);
            connection.setDoOutput(messageBody != null);

            acceptListToHeader(connection, HttpHelper.HEADER_ACCEPT, transportDescription.getAcceptedMediaFormats());
            acceptListToHeader(connection, HttpHelper.HEADER_ACCEPT_LANGUAGE, transportDescription.getAcceptedLanguages());
            connection.setRequestProperty("Accept-Charset", "utf-8, *; q=0");

            if (messageBody != null) {
                final @NotNull OutputStream outputStream;

                connection.setRequestProperty(HttpHelper.HEADER_CONTENT_TYPE, messageBody.getMediaType());

                outputStream = connection.getOutputStream();
                outputStream.write(messageBody.getBytes());
                outputStream.close();
            }

            connection.connect();

            inputStream = connection.getInputStream();
            contentType = connection.getContentType();
        } catch (Exception e) {
            transportDescription.signalConnectionState(TransportConnectionState.ERROR);
            throw e;
        }

        transportDescription.signalConnectionState(TransportConnectionState.CONNECTED);

        {
            final @NotNull Sync.Builder builder = new Sync.Builder(transportDescription.getSource());
            builder.setCurrentService(new ICEBasedService(transportDescription.getSource(), transportDescription.getInitialService().getIdentifier(), getHeadersAsMap(connection)));
            builder.setTemporalValidity(TemporalValidity.INDEFINITELY_VALID);
            sync = builder.build();
            transportDescription.getMetadataMixer().accept(sync);
        }
    }

    @Override
    public @NotNull ByteDataBlock read() throws IOException {
        //noinspection MagicNumber
        return new ByteDataBlock(sync, null, inputStream, 1024 * 2);
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
        transportDescription.signalConnectionState(TransportConnectionState.DISCONNECTING);
        inputStream.close();
        transportDescription.signalConnectionState(TransportConnectionState.DISCONNECTED);
    }
}
