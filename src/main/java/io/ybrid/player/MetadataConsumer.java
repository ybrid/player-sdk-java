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

import io.ybrid.api.Metadata;

/**
 * This interface is implemented by classes that can consume ybrid {@link Metadata}.
 */
public interface MetadataConsumer {
    /**
     * This function is called by the {@link MetadataProvider} when there is no {@link Metadata}
     * to be consumed.
     *
     * @param metadata The {@link Metadata} to consume.
     */
    void onMetadataChange(Metadata metadata);
}
