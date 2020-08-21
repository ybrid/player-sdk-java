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

import io.ybrid.api.ClockManager;
import io.ybrid.api.MetadataMixer;
import io.ybrid.api.Session;
import io.ybrid.api.metadata.InvalidMetadata;
import io.ybrid.api.metadata.Metadata;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

class ICYInputStream implements Closeable, ByteDataSource {
    static final Logger LOGGER = Logger.getLogger(ICYInputStream.class.getName());
    private static final String HEADER_ICY_METAINT = "icy-metaint"; //NON-NLS
    private static final int MAX_READ_LENGTH = 4*1024;
    private static final int MAX_METATDATA_INTERVAL = 128*1024;
    private static final int READ_BUFFER_LENGTH = 2048;
    private static final int ICY_METADATA_BLOCK_MULTIPLAYER = 16;
    private final Session session;
    private final String host;
    private int port;
    private final String path;
    private final boolean secure;
    private Socket socket;
    private InputStream inputStream;
    private HashMap<String, String> replyHeaders;
    private int metadataInterval;
    private int pos = 0;
    private ICYMetadata metadata;
    private boolean metadataUpdated = false;
    private Metadata blockMetadata = null;

    @SuppressWarnings("HardCodedStringLiteral")
    public ICYInputStream(@NotNull Session session) throws MalformedURLException {
        URI uri = session.getStreamURI();

        this.session = session;

        System.out.println("url = " + uri);

        switch (uri.getScheme()) {
            case "icyx":
                secure = false;
                break;
                /* Workarounds */
            case "http":
                secure = false;
                break;
            case "https":
                secure = true;
                break;
            default:
                throw new MalformedURLException("Invalid protocol: " + uri.getScheme());
        }

        host = uri.getHost();
        port = uri.getPort();
        if (port < 1)
            port = uri.toURL().getDefaultPort();
        if (uri.getQuery().isEmpty()) {
            path = uri.getPath();
        } else {
            path = uri.getPath() + "?" + uri.getQuery();
        }
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
        String req = "GET " + path + " HTTP/1.0\r\n";

        req += "Host: " + host + ":" + port + "\r\n";
        req += "Connection: close\r\n";
        req += "User-Agent: Ybrid Player\r\n";
        req += acceptListToHeader(HttpHelper.HEADER_ACCEPT, session.getAcceptedMediaFormats());
        req += acceptListToHeader(HttpHelper.HEADER_ACCEPT_LANGUAGE, session.getAcceptedLanguages());
        req += "Accept-Charset: utf-8, *; q=0\r\n";
        req += "Accept-Encoding: identity, *; q=0\r\n";
        req += "Icy-MetaData: 1\r\n";
        req += "\r\n";

        socket.getOutputStream().write(req.getBytes(StandardCharsets.US_ASCII));
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
                String[] status = line.split(" ");
                if (!status[1].equals("200"))
                    throw new IOException("Bad reply");
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

    synchronized void connect() throws IOException {
        if (inputStream != null)
            return;

        if (secure) {
            socket = SSLSocketFactory.getDefault().createSocket(host, port);
        } else {
            socket = new Socket(host, port);
        }
        sendRequest();
        receiveReply();
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

        length *= ICY_METADATA_BLOCK_MULTIPLAYER;
        rawMetadata = new byte[length];
        ret = inputStream.read(rawMetadata);
        if (ret != length)
            throw new IOException("Can not read body: length = " + length + ", ret = " + ret);

        metadata = new ICYMetadata(rawMetadata);
        LOGGER.info("Got fresh metadata: " + metadata); //NON-NLS
        LOGGER.info("Item: " + metadata.asItem()); //NON-NLS
        metadataUpdated = true;
        session.getMetadataMixer().add(metadata.asItem(), MetadataMixer.Source.TRANSPORT, MetadataMixer.Position.CURRENT, null, ClockManager.now());
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

    ICYMetadata getMetadata() {
        return metadata;
    }

    @Override
    public synchronized void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
            inputStream = null;
        }
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    @Override
    public @NotNull ByteDataBlock read() throws IOException {
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

        if (metadataUpdated)
            blockMetadata = new InvalidMetadata();

        block = new ByteDataBlock(blockMetadata, null, inputStream, todo);

        pos += block.getData().length;

        if (blockMetadata != null)
            metadataUpdated = false;

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
