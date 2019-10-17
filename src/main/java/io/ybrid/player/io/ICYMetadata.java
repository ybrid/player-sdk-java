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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ICYMetadata {
    private byte[] raw;
    private byte[] reduced;
    private Map<String, String> values = new HashMap<>();

    ICYMetadata(byte[] raw) {
        this.raw = raw;
        parse();
    }

    private int parseKeyValue(byte[] input, int offset) {
        String key = null;
        String value = null;
        byte lookingFor = '=';
        int valueStart = -1;

        for (int i = offset; i < input.length; i++) {
            if (input[i] != lookingFor)
                continue;

            if (key == null) {
                key = new String(input, offset, (i - offset), StandardCharsets.UTF_8);
                if (i == (input.length - 1))
                    return input.length;
                i++;
                if (input[i] == '\'' || input[i] == '\"') {
                    lookingFor = input[i];
                } else {
                    lookingFor = ';';
                }
                valueStart = i + 1;
            } else {
                value = new String(input, valueStart, (i - valueStart), StandardCharsets.UTF_8);

                if (input[i] != ';')
                    i++;

                if (input.length >= (i + 1) && input[i] == ';')
                    i++;

                values.put(key, value);
                return i;
            }
        }

        return input.length;
    }

    private void parse() {
        for (int i = 0; i < raw.length; i++) {
            if (raw[i] == 0) {
                reduced = new byte[i];
                System.arraycopy(raw, 0, reduced, 0, i);
                break;
            }
        }
        if (reduced == null)
            reduced = raw;

        for (int offset = 0; offset >= 0 && offset < reduced.length; )
            offset = parseKeyValue(reduced, offset);
    }

    byte[] getRaw() {
        return raw;
    }

    Map<String, String> getValues() {
        return Collections.unmodifiableMap(values);
    }

    String get(String key) {
        return getValues().get(key);
    }

    @Override
    public String toString() {
        return "ICYMetadata{" +
                "raw.length=" + raw.length +
                ", new String(reduced)=" + new String(reduced) +
                ", reduced=" + Arrays.toString(reduced) +
                ", values=" + values +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ICYMetadata metadata = (ICYMetadata) o;
        return Arrays.equals(raw, metadata.raw);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(raw);
    }
}
