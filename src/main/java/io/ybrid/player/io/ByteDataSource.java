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

import java.io.IOException;

/**
 * This interface is implemented by classes that allow reading {@link ByteDataBlock ByteDataBlocks}.
 */
public interface ByteDataSource extends DataSource {
    /**
     * Read a block.
     *
     * @return The block that has been read.
     * @throws IOException And I/O-Errors occurred while reading the block.
     */
    ByteDataBlock read() throws IOException;

    /**
     * Return the content-type of the current stream.
     *
     * Calling this may connect if this source is {@link io.ybrid.api.Connectable}.
     *
     * @return The content-type or null.
     */
    String getContentType();
}