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

package io.ybrid.player.io.protocol;

import org.jetbrains.annotations.Contract;

/**
 * This class only stores common constants used by HTTP.
 * See RFCs 7230 to 7235 for reference.
 */
@SuppressWarnings("HardCodedStringLiteral")
final class HttpHelper {
    static final int STATUS_MIN = 100;
    static final int STATUS_MAX = 599;
    static final int STATUS_PERMANENT_MIN = 200;
    static final int STATUS_PERMANENT_MAX = STATUS_MAX;

    static final int STATUS_OK = 200;
    static final int STATUS_MOVED_PERMANENTLY = 301;
    static final int STATUS_FOUND = 302;
    static final int STATUS_SEE_OTHER = 303;
    static final int STATUS_TEMPORARY_REDIRECT = 307;
    static final int STATUS_PERMANENT_REDIRECT = 308;

    static final String HEADER_CONTENT_TYPE = "Content-Type";
    static final String HEADER_ACCEPT = "Accept";
    static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    static final String HEADER_LOCATION = "Location";

    /**
     * Returns whether the given HTTP status code indicates a redirect.
     * @param status The status code to test.
     * @return Whether the status indicates a redirect.
     */
    @Contract(pure = true)
    static boolean isRedirect(int status) {
        if (status < STATUS_MIN || status > STATUS_MAX)
            throw new IllegalArgumentException("Status is out of range: " + status);

        switch (status) {
            case STATUS_MOVED_PERMANENTLY:
            case STATUS_FOUND:
            case STATUS_SEE_OTHER:
            case STATUS_TEMPORARY_REDIRECT:
            case STATUS_PERMANENT_REDIRECT:
                return true;
            default:
                return false;
        }
    }
}
