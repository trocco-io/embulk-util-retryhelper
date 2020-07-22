package org.embulk.util.retryhelper;

public class RetryExecutor {
    private RetryExecutor(int retryLimit, int initialRetryWait, int maxRetryWait) {
        this.retryLimit = retryLimit;
        this.initialRetryWait = initialRetryWait;
        this.maxRetryWait = maxRetryWait;
    }

    public static RetryExecutor retryExecutor() {
        // TODO default configuration
        return new RetryExecutor(3, 500, 30 * 60 * 1000);
    }

    public RetryExecutor withRetryLimit(int count) {
        return new RetryExecutor(count, initialRetryWait, maxRetryWait);
    }

    public RetryExecutor withInitialRetryWait(int msec) {
        return new RetryExecutor(retryLimit, msec, maxRetryWait);
    }

    public RetryExecutor withMaxRetryWait(int msec) {
        return new RetryExecutor(retryLimit, initialRetryWait, msec);
    }

    public <T> T runInterruptible(Retryable<T> op)
            throws InterruptedException, RetryGiveupException {
        return run(op, true);
    }

    public <T> T run(Retryable<T> op) throws RetryGiveupException {
        try {
            return run(op, false);
        } catch (InterruptedException ex) {
            throw new RetryGiveupException("Unexpected interruption", ex);
        }
    }

    private <T> T run(Retryable<T> op, boolean interruptible) throws InterruptedException, RetryGiveupException {
        int retryWait = initialRetryWait;
        int retryCount = 0;

        Exception firstException = null;

        while (true) {
            try {
                return op.call();
            } catch (Exception exception) {
                if (firstException == null) {
                    firstException = exception;
                }
                if (!op.isRetryableException(exception) || retryCount >= retryLimit) {
                    op.onGiveup(firstException, exception);
                    throw new RetryGiveupException(firstException);
                }

                retryCount++;
                op.onRetry(exception, retryCount, retryLimit, retryWait);

                try {
                    Thread.sleep(retryWait);
                } catch (InterruptedException ex) {
                    if (interruptible) {
                        throw ex;
                    }
                }

                // exponential back-off with hard limit
                retryWait *= 2;
                if (retryWait > maxRetryWait) {
                    retryWait = maxRetryWait;
                }
            }
        }
    }

    private final int retryLimit;
    private final int initialRetryWait;
    private final int maxRetryWait;
}
