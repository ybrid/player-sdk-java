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
import io.ybrid.player.io.RealBlockingInputStream;
import org.jetbrains.annotations.*;

import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

@ApiStatus.Internal
public class ICYInputStream implements Closeable, ByteDataSource {
    static final @NonNls Logger LOGGER = Logger.getLogger(ICYInputStream.class.getName());
    private static final String HEADER_ICY_METAINT = "icy-metaint"; //NON-NLS
    private static final int MAX_READ_LENGTH = 4*1024;
    private static final int MAX_METATDATA_INTERVAL = 128*1024;
    private static final int READ_BUFFER_LENGTH = 2048;
    private static final int ICY_METADATA_BLOCK_MULTIPLAYER = 16;
    private final @NotNull ServiceURITransportDescription transportDescription;
    private Socket socket;
    private InputStream inputStream;
    private HashMap<String, String> replyHeaders;
    private int metadataInterval;
    private int pos = 0;
    private Sync sync = null;
    private int status = 0;
    private @NotNull URI uri;

    private int getPort() throws MalformedURLException {
        int port = uri.getPort();

        if (port < 1)
            port = uri.toURL().getDefaultPort();

        return port;
    }

    @SuppressWarnings("HardCodedStringLiteral")
    public ICYInputStream(@NotNull ServiceURITransportDescription transportDescription) throws MalformedURLException {
        uri = transportDescription.getURI();

        this.transportDescription = transportDescription;

        LOGGER.info("URL = " + uri);
    }

    private static @NotNull String acceptListToHeader(@NotNull String header, @Nullable Map<String, Double> list) {
        @NonNls StringBuilder ret = new StringBuilder();

        if (list == null || list.isEmpty())
            return "";

        for (Map.Entry<String, Double> entry : list.entrySet()) {
            if (ret.length() > 0)
                ret.append(", ");
            ret.append(entry.getKey()).append("; q=").append(entry.getValue());
        }
        return header + ": " + ret + "\r\n";
    }

    @SuppressWarnings("HardCodedStringLiteral")
    private void sendRequest() throws IOException {
        final @NotNull OutputStream outputStream;
        final @Nullable MessageBody messageBody = transportDescription.getRequestBody();
        final @Nullable byte[] body;
        final @NotNull String path;
        String req;

        if (uri.getQuery() == null || uri.getQuery().isEmpty()) {
            path = uri.getPath();
        } else {
            path = uri.getPath() + "?" + uri.getQuery();
        }

        req = "GET " + path + " HTTP/1.0\r\n";

        req += "Host: " + uri.getHost() + ":" + getPort() + "\r\n";
        req += "Connection: close\r\n";
        req += "User-Agent: Ybrid Player\r\n";
        req += acceptListToHeader(HttpHelper.HEADER_ACCEPT, transportDescription.getAcceptedMediaFormats());
        req += acceptListToHeader(HttpHelper.HEADER_ACCEPT_LANGUAGE, transportDescription.getAcceptedLanguages());
        req += "Accept-Charset: utf-8, *; q=0\r\n";
        req += "Accept-Encoding: identity, *; q=0\r\n";
        req += "Icy-MetaData: 1\r\n";

        if (messageBody != null) {
            req += HttpHelper.HEADER_CONTENT_TYPE + ": " + messageBody.getMediaType() + "\r\n";
            body = messageBody.getBytes();
            req += "Content-length: " + body.length + "\r\n";
        } else {
            body = null;
        }

        req += "\r\n";

        outputStream = socket.getOutputStream();
        outputStream.write(req.getBytes(StandardCharsets.US_ASCII));
        if (body != null)
            outputStream.write(body);
    }

    @Contract("_ -> new")
    @SuppressWarnings("MagicCharacter")
    private static @NotNull String getHeader(@NotNull InputStream inputStream) throws IOException {
        int nextLength = READ_BUFFER_LENGTH;
        byte[] buffer;
        int ret;

        inputStream.mark(0);

        outer:
        for (int iter = 0; iter < 8; iter++) {
            buffer = new byte[nextLength];
            inputStream.reset();
            inputStream.mark(buffer.length);
            ret = inputStream.read(buffer);

            if (ret < 1) {
                inputStream.reset();
                throw new IOException("Can not read from input, got " + ret + " byte");
            }

            for (int i = 3; i < ret; i++) {
                if (buffer[i-3] == '\r' && buffer[i-2] == '\n' &&
                buffer[i-1] == '\r' && buffer[i] == '\n') {
                    if (i == (nextLength - 1)) {
                        return new String(buffer, StandardCharsets.UTF_8);
                    } else {
                        nextLength = i + 1;
                        continue outer;
                    }
                }
            }

            nextLength += READ_BUFFER_LENGTH;
        }

        inputStream.reset();
        throw new IOException("Can not find end of header");
    }

    private @NotNull HashMap<String, String> parseHeader(@NotNull String header) throws IOException {
        HashMap<String, String> ret = new HashMap<>();
        boolean firstLine = true;

        for (String line : header.split("\r\n")) {
            String[] kv;

            if (firstLine) {
                String[] statusLine = line.split(" ");
                status = Integer.parseInt(statusLine[1], 10);
                if (status < HttpHelper.STATUS_PERMANENT_MIN || status > HttpHelper.STATUS_PERMANENT_MAX)
                    throw new IOException("Bad reply with status " + status);
                firstLine = false;
                continue;
            }

            kv = line.split(": ?", 2);
            ret.put(kv[0].toLowerCase(), kv[1]);
        }

        return ret;
    }

    private void receiveReply() throws IOException {
        InputStream inputStream = new RealBlockingInputStream(new BufferedInputStream(socket.getInputStream()));
        String header = getHeader(inputStream);
        String metaInt;

        replyHeaders = parseHeader(header);

        metaInt = replyHeaders.get(HEADER_ICY_METAINT);
        if (metaInt == null) {
            metadataInterval = -1;
        } else {
            metadataInterval = Integer.parseInt(metaInt);
            if (metadataInterval < 0 || metadataInterval > MAX_METATDATA_INTERVAL)
                throw new IOException("Invalid metadata interval");
        }

        this.inputStream = inputStream;
    }

    private synchronized void connect() throws IOException {
        if (inputStream != null)
            return;

        transportDescription.signalConnectionState(TransportConnectionState.CONNECTING);

        try {
            for (int connection = 0; connection < 3; connection++) {
                switch (uri.getScheme()) {
                    case "http": //NON-NLS
                        LOGGER.warning("Invalid protocol " + uri.getScheme() + ", guessing icyx"); //NON-NLS
                    case "icyx": //NON-NLS
                        socket = new Socket(uri.getHost(), getPort());
                        break;
                    case "https": //NON-NLS
                        LOGGER.warning("Invalid protocol " + uri.getScheme() + ", guessing icyxs"); //NON-NLS
                    case "icyxs": //NON-NLS
                        socket = SSLSocketFactory.getDefault().createSocket(uri.getHost(), getPort());
                        break;
                    default:
                        throw new MalformedURLException("Invalid protocol: " + uri.getScheme());
                }
                sendRequest();
                receiveReply();
                LOGGER.info("ICY Request to " + uri + " returned " + status + " [" + getContentType() + "]"); //NON-NLS

                if (HttpHelper.isRedirect(status)) {
                    final @Nullable String location = replyHeaders.get(HttpHelper.HEADER_LOCATION.toLowerCase(Locale.ROOT));
                    LOGGER.warning("Got redirect from " + uri + " to " + location);
                    if (location != null && !location.isEmpty()) {
                        try {
                            final @NotNull URI locationURI = new URI(location);
                            if (locationURI.isAbsolute()) {
                                uri = locationURI;
                            } else {
                                throw new IOException("Unsupported redirect from " + uri + " to " + location);
                            }
                        } catch (URISyntaxException e) {
                            throw new IOException(e);
                        }

                        close();
                        continue;
                    }
                }
                break;
            }

            if (status != HttpHelper.STATUS_OK)
                throw new IOException("Bad ICY reply with unexpected status " + status);
        } catch (Exception e) {
            transportDescription.signalConnectionState(TransportConnectionState.ERROR);
            throw e;
        }

        transportDescription.signalConnectionState(TransportConnectionState.CONNECTED);

        {
            final @NotNull Sync.Builder builder = new Sync.Builder(transportDescription.getSource());
            builder.setCurrentService(new ICEBasedService(transportDescription.getSource(), transportDescription.getInitialService().getIdentifier(), replyHeaders));
            builder.setTemporalValidity(TemporalValidity.INDEFINITELY_VALID);
            sync = builder.build();
            transportDescription.getMetadataMixer().accept(sync);
        }
    }


    private void readMetadataInner() throws IOException {
        int length = inputStream.read();
        byte[] rawMetadata;
        int ret;

        if (length < 0) {
            close();
            throw new IOException("Can not read length.");
        }

        if (length == 0)
            return;

        LOGGER.info("Metadata Length header value: " + length + " (" + length*16 + " byte)");

        length *= ICY_METADATA_BLOCK_MULTIPLAYER;
        rawMetadata = new byte[length];
        ret = inputStream.read(rawMetadata);
        if (ret != length)
            throw new IOException("Can not read body: length = " + length + ", ret = " + ret);

        {
            final @NotNull Sync.Builder builder = new Sync.Builder(sync);
            final @NotNull ICYMetadata metadata = new ICYMetadata(transportDescription.getSource(), rawMetadata);
            builder.setCurrentTrack(metadata);
            builder.autoFill();
            sync = builder.build();
            transportDescription.getMetadataMixer().accept(sync);
            LOGGER.info("Got fresh metadata: " + metadata); //NON-NLS
        }
    }

    private void readMetadata() throws IOException {
        try {
            readMetadataInner();
        } catch (IOException e) {
            throw new IOException("Error reading Metadata. VERY BAD. Stream closed.", e);
        }
    }

    Map<String, String> getReplyHeaders() {
        return Collections.unmodifiableMap(replyHeaders);
    }

    @Override
    public synchronized void close() throws IOException {
        transportDescription.signalConnectionState(TransportConnectionState.DISCONNECTING);
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
        transportDescription.signalConnectionState(TransportConnectionState.DISCONNECTED);
    }

    @Override
    public synchronized @NotNull ByteDataBlock read() throws IOException {
        ByteDataBlock block;
        int todo;

        connect();

        if (metadataInterval > 0) {
            todo = metadataInterval - pos;
        } else {
            todo = MAX_READ_LENGTH;
        }

        if (todo > MAX_READ_LENGTH)
            todo = MAX_READ_LENGTH;

        block = new ByteDataBlock(sync, null, inputStream, todo);

        pos += block.getData().length;

        if (metadataInterval > 0 && pos == metadataInterval) {
            pos = 0;
            readMetadata();
        }

        return block;
    }

    @Override
    public boolean isValid() {
        return inputStream != null;
    }

    @Override
    public String getContentType() {
        try {
            connect();
        } catch (IOException ignored) {
        }

        if (replyHeaders == null)
            return null;

        return replyHeaders.get(HttpHelper.HEADER_CONTENT_TYPE.toLowerCase(Locale.ROOT));
    }
}
