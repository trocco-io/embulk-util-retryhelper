package org.embulk.util.retryhelper;

import java.util.concurrent.ExecutionException;

public final class RetryGiveupException extends ExecutionException {
    public RetryGiveupException(final String message, final Exception cause) {
        super(message, cause);
    }

    public RetryGiveupException(final Exception cause) {
        super(cause);
    }

    @Override
    public Exception getCause() {
        return (Exception) super.getCause();
    }
}
