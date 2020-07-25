package org.embulk.util.retryhelper.jetty92;

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
