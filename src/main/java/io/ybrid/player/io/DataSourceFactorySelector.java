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

package io.ybrid.player.io;

import io.ybrid.api.transport.ServiceTransportDescription;
import io.ybrid.api.transport.ServiceURITransportDescription;
import io.ybrid.player.io.protocol.ICYInputStream;
import io.ybrid.player.io.protocol.URLSource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements a {@link DataSourceFactory} that selects the correct factory based on the passed {@link ServiceTransportDescription}.
 */
public final class DataSourceFactorySelector implements DataSourceFactory {
    private final @NotNull Logger LOGGER = Logger.getLogger(DataSourceFactorySelector.class.getName());

    /**
     * This interface is used for references to simple providers.
     * A simple provider is a method that provides a data source but does not implement the overhead of a {@link DataSourceFactory}.
     */
    public interface SimpleProvider {
        /**
         * This gets a new {@link ByteDataSource} for the provided {@link ServiceURITransportDescription}.
         * This is normally the prototype of implementing class' constructors.
         *
         * @param uriTransportDescription The transport description to use.
         * @return The new {@link ByteDataSource}.
         * @throws IOException Any exception as thrown by creation of the {@link ByteDataSource}.
         */
        @NotNull ByteDataSource getSource(@NotNull ServiceURITransportDescription uriTransportDescription) throws IOException;
    }

    private final @NotNull Map<String, SimpleProvider> schemes = new HashMap<>();
    private final @Nullable SimpleProvider fallbackSource;

    private DataSourceFactorySelector(@Nullable SimpleProvider fallbackSource) {
        this.fallbackSource = fallbackSource;
    }

    /**
     * Creates a instance with default factories already installed.
     * @return The new instance.
     */
    public static @NotNull DataSourceFactorySelector createWithDefaults() {
        final @NotNull DataSourceFactorySelector selector = new DataSourceFactorySelector(URLSource::new);
        selector.register("icyx", ICYInputStream::new);
        selector.register("icyxs", ICYInputStream::new);
        return selector;
    }

    /**
     * Creates a instance with no default factories installed.
     * @param fallbackSource The default {@link SimpleProvider} to use if no other factory can handle a request.
     * @return The new instance.
     */
    public static @NotNull DataSourceFactorySelector createWithoutDefaults(@Nullable SimpleProvider fallbackSource) {
        return new DataSourceFactorySelector(fallbackSource);
    }

    /**
     * Registers a new {@link SimpleProvider} for a given scheme.
     * @param scheme The scheme to register for.
     * @param provider The {@link SimpleProvider} to register. Normally this is a reference to a class' constructor.
     */
    public void register(@NotNull String scheme, @NotNull SimpleProvider provider) {
        schemes.put(scheme.toLowerCase(Locale.ROOT), provider);
    }

    private @NotNull ByteDataSource getSourceByURI(@NotNull ServiceURITransportDescription transportDescription) throws IOException {
        final @NotNull URI uri = transportDescription.getURI();
        final @NotNull String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
        final @Nullable SimpleProvider provider;

        LOGGER.log(Level.INFO, "getSourceByFallback(transportDescription=" + transportDescription + "): uri=" + uri); //NON-NLS

        provider = schemes.get(scheme);
        if (provider != null)
            return provider.getSource(transportDescription);

        if (fallbackSource != null)
            return fallbackSource.getSource(transportDescription);

        throw new UnsupportedOperationException("Scheme not supported: " + scheme);
    }

    @Override
    public @NotNull ByteDataSource getSource(@NotNull ServiceTransportDescription transportDescription) throws IOException {
        if (transportDescription instanceof ServiceURITransportDescription) {
            return getSourceByURI((ServiceURITransportDescription) transportDescription);
        } else {
            throw new IllegalArgumentException("Unsupported transport description: " + transportDescription);
        }
    }
}
