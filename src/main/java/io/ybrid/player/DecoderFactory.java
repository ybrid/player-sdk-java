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

package io.ybrid.player;

import io.ybrid.player.io.DataSource;

/**
 * This interface is implemented by factory classes that build {@link Decoder} instances.
 */
public interface DecoderFactory {
    /**
     * Build a new {@link Decoder} based on the dataSource provided.
     *
     * This must not call {@link DataSource#read()} on the provided {@link DataSource}.
     *
     * @param dataSource The {@link DataSource} used to read data from.
     * @return The {@link Decoder} that has been build.
     */
    Decoder getDecoder(DataSource dataSource);
}
