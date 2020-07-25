package org.embulk.util.retryhelper;

import java.util.concurrent.Callable;

public interface Retryable<T> extends Callable<T> {
    @Override
    T call() throws Exception;

    boolean isRetryableException(Exception exception);

    void onRetry(Exception exception, int retryCount, int retryLimit, int retryWait) throws RetryGiveupException;

    void onGiveup(Exception firstException, Exception lastException) throws RetryGiveupException;
}
