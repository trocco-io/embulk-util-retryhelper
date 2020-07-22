/*
 * Copyright 2020 The Embulk project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.embulk.util.retryhelper.jetty93;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

final class Util {
    private Util() {
        // No instantiation.
    }

    static String getStackTraceAsString(final Throwable throwable) {
        final StringWriter errors = new StringWriter();
        throwable.printStackTrace(new PrintWriter(errors));
        return errors.toString();
    }

    static String asString(final InputStream inputStream) throws IOException {
        try {
            return asString(inputStream, StandardCharsets.UTF_8);
        } catch (final UnsupportedEncodingException ex) {
            throw new UncheckedIOException("Unexpected failure: UTF-8 should be supported.", ex);
        }
    }

    static String asString(final InputStream inputStream, final Charset charset) throws IOException {
        return asByteArrayOutputStream(inputStream).toString(charset.toString());
    }

    static ByteArrayOutputStream asByteArrayOutputStream(final InputStream inputStream) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final byte[] buffer = new byte[1024];
        while (true) {
            final int length = inputStream.read(buffer);
            if (length < 0) {
                break;
            }
            output.write(buffer, 0, length);
        }
        return output;
    }
}
