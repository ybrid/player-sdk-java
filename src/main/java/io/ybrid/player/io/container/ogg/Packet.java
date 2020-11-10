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

package io.ybrid.player.io.container.ogg;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class Packet implements hasGranularPosition {
    private final @NotNull GranularPosition granularPosition;
    private final @NotNull EnumSet<Flag> flags;
    private final @NotNull byte[] body;
    private final boolean afterHole;

    public Packet(@NotNull GranularPosition granularPosition, @NotNull Set<Flag> flags, @NotNull byte[] body, boolean afterHole) {
        this.afterHole = afterHole;
        if (flags.contains(Flag.CONTINUED))
            throw new IllegalArgumentException("Invalid flags passed: " + flags);

        this.granularPosition = granularPosition;
        this.flags = EnumSet.copyOf(flags);
        this.body = body;
    }

    @Override
    public @NotNull GranularPosition getGranularPosition() {
        return granularPosition;
    }

    public @NotNull @UnmodifiableView Set<Flag> getFlags() {
        return Collections.unmodifiableSet(flags);
    }

    public byte[] getBody() {
        return body;
    }

    public boolean isAfterHole() {
        return afterHole;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "granularPosition=" + granularPosition +
                ", flags=" + flags +
                ", afterHole=" + afterHole +
                ", body=" + (body[0] == 'O' ? new String(body, StandardCharsets.UTF_8) : "<binary>") +
                '}';
    }
}
