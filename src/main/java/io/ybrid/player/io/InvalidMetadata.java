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

import io.ybrid.api.Item;
import io.ybrid.api.Metadata;
import io.ybrid.api.Service;
import io.ybrid.api.SwapInfo;

final class InvalidMetadata implements Metadata {
    @Override
    public Item getCurrentItem() {
        return null;
    }

    @Override
    public Item getNextItem() {
        return null;
    }

    @Override
    public int getCurrentBitRate() {
        return -1;
    }

    @Override
    public Service getService() {
        return null;
    }

    @Override
    public SwapInfo getSwapInfo() {
        return null;
    }

    @Override
    public long getTimeToNextItem() {
        return -1;
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
