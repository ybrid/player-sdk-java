/*
 * Copyright (c) 2020 nacamar GmbH - Ybrid®, a Hybrid Dynamic Live Audio Technology
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

import io.ybrid.api.metadata.Sync;
import io.ybrid.api.metadata.source.Source;
import io.ybrid.api.metadata.source.SourceType;
import io.ybrid.api.util.MediaType;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * This implements a {@link ByteDataSource} based on local files.
 */
public class FileByteDataSource implements ByteDataSource {
    private final @NotNull InputStream inputStream;
    private final @NotNull Sync sync;
    private final @NotNull MediaType mediaType;

    /**
     * Main constructor.
     *
     * @param filename The name of the file to open.
     * @param mediaType The media type of the file.
     * @throws FileNotFoundException Thrown as per {@link FileInputStream#FileInputStream(String)}.
     */
    public FileByteDataSource(@NotNull String filename, @NotNull MediaType mediaType) throws FileNotFoundException {
        this.mediaType = mediaType;
        this.inputStream = new FileInputStream(filename);
        this.sync = new Sync.Builder(new Source(SourceType.TRANSPORT)).build();
    }

    /**
     * Creates a instance with a media type of {@code "application/octet-stream"},
     *
     * @param filename The name of the file to open.
     * @throws FileNotFoundException Thrown as per {@link FileInputStream#FileInputStream(String)}.
     */
    public FileByteDataSource(@NotNull String filename) throws FileNotFoundException {
        this(filename, MediaType.MEDIA_TYPE_APPLICATION_OCTET_STREAM);
    }

    @Override
    public @NotNull ByteDataBlock read() throws IOException {
        return new ByteDataBlock(sync, null, inputStream, 1024);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public @NotNull MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
