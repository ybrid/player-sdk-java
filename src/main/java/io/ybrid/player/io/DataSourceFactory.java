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

import io.ybrid.api.transport.*;
import io.ybrid.player.io.protocol.ICYInputStream;
import io.ybrid.player.io.protocol.URLSource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This factory is used to build {@link DataSource DataSources}.
 */
public final class DataSourceFactory {
    static final Logger LOGGER = Logger.getLogger(DataSourceFactory.class.getName());

    /**
     * This builds a {@link ByteDataSource} for the audio stream based on a {@link ServiceTransportDescription}.
     *
     * @param transportDescription The {@link ServiceTransportDescription} to use.
     * @return The {@link ByteDataSource} for the stream.
     * @throws IOException I/O-Errors as thrown by the used backends.
     */
    public static ByteDataSource getSourceByTransportDescription(@NotNull ServiceTransportDescription transportDescription) throws IOException {
        if (transportDescription instanceof ServiceURITransportDescription) {
            final @NotNull ServiceURITransportDescription uriTransportDescription = (ServiceURITransportDescription) transportDescription;
            final @NotNull URI uri = uriTransportDescription.getURI();
            final @NotNull String scheme = uri.getScheme();

            LOGGER.log(Level.INFO, "getSourceByTransportDescription(transportDescription=" + transportDescription + "): uri=" + uri); //NON-NLS

            if (scheme.equals("icyx") || scheme.equals("icyxs")) { //NON-NLS
                try {
                    return new ICYInputStream(uriTransportDescription);
                } catch (MalformedURLException ignored) {
                }
            }

            return new URLSource(uriTransportDescription);
        } else {
            throw new IllegalArgumentException("Unsupported transport description: " + transportDescription);
        }
    }
}
