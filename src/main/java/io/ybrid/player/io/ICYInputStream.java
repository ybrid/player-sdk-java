/*
 * Copyright (c) 2019 nacamar GmbH - YBRID®, a Hybrid Dynamic Live Audio Technology
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

import io.ybrid.api.Metadata;

import javax.net.ssl.SSLSocketFactory;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ICYInputStream implements Closeable, ByteDataSource {
    private static final int MAX_READ_LENGTH = 4*1024;
    private String host;
    private int port;
    private String path;
    private boolean secure;
    private Socket socket;
    private InputStream inputStream;
    private HashMap<String, String> replyHeaders;
    private int metadataInterval;
    private int pos = 0;
    private ICYMetadata metadata;
    private boolean metadataUpdated = false;
    private Metadata blockMetadata = null;

    public ICYInputStream(URL url) throws MalformedURLException {
        switch (url.getProtocol()) {
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
                throw new MalformedURLException("Invalid protocol: " + url.getProtocol());
        }

        host = url.getHost();
        port = url.getPort();
        if (port < 1)
            port = url.getDefaultPort();
        path = url.getFile();
    }

    private void sendRequest() throws IOException {
        String req = "GET " + path + " HTTP/1.0\r\n";

        req += "Host: " + host + "\r\n";
        req += "Connection: close\r\n";
        req += "User-Agent: ybrid Player\r\n";
        req += "Icy-MetaData: 1\r\n";
        req += "\r\n";

        socket.getOutputStream().write(req.getBytes());
    }

    private String getHeader(InputStream inputStream) throws IOException {
        int nextLength = 2048;
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

            nextLength += 2048;
        }

        inputStream.reset();
        throw new IOException("Can not find end of header");
    }

    private HashMap<String, String> parseHeader(String header) throws IOException {
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
        InputStream inputStream = new BufferedInputStream(socket.getInputStream());
        String header = getHeader(inputStream);
        String metaInt;

        replyHeaders = parseHeader(header);

        metaInt = replyHeaders.get("icy-metaint");
        if (metaInt == null) {
            metadataInterval = -1;
        } else {
            metadataInterval = Integer.parseInt(metaInt);
            if (metadataInterval < 0 || metadataInterval > (128*1024))
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
        byte[] metadata;
        int ret;

        if (length < 0) {
            close();
            throw new IOException("Can not read length.");
        }

        if (length == 0)
            return;

        length *= 16;
        metadata = new byte[length];
        ret = inputStream.read(metadata);
        if (ret != length)
            throw new IOException("Can not read body: length = " + length + ", ret = " + ret);

        this.metadata = new ICYMetadata(metadata);
        metadataUpdated = true;
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
    public ByteDataBlock read() throws IOException {
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

        block = new ByteDataBlock(blockMetadata, inputStream, todo);

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

        return replyHeaders.get("content-type");
    }
}
