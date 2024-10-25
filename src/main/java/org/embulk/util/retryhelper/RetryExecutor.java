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

package org.embulk.util.retryhelper;

public class RetryExecutor {
    private RetryExecutor(final int retryLimit, final int initialRetryWait, final int maxRetryWait) {
        this.retryLimit = retryLimit;
        this.initialRetryWait = initialRetryWait;
        this.maxRetryWait = maxRetryWait;
    }

    public static RetryExecutor ofDefault() {
        return new RetryExecutor(
                DEFAULT_RETRY_LIMIT,
                DEFAULT_INITIAL_RETRY_WAIT_MILLIS,
                DEFAULT_MAX_RETRY_WAIT_MILLIS);
    }

    @Deprecated
    public static RetryExecutor retryExecutor() {
        return ofDefault();
    }

    @Deprecated
    public RetryExecutor withRetryLimit(int count) {
        return new RetryExecutor(count, this.initialRetryWait, this.maxRetryWait);
    }

    @Deprecated
    public RetryExecutor withInitialRetryWait(int msec) {
        return new RetryExecutor(this.retryLimit, msec, this.maxRetryWait);
    }

    @Deprecated
    public RetryExecutor withMaxRetryWait(int msec) {
        return new RetryExecutor(this.retryLimit, this.initialRetryWait, msec);
    }

    public static class Builder {
        Builder() {
            this.retryLimit = DEFAULT_RETRY_LIMIT;
            this.initialRetryWaitMillis = DEFAULT_INITIAL_RETRY_WAIT_MILLIS;
            this.maxRetryWaitMillis = DEFAULT_MAX_RETRY_WAIT_MILLIS;
        }

        public Builder withRetryLimit(final int retryLimit) {
            this.retryLimit = retryLimit;
            return this;
        }

        public Builder withInitialRetryWaitMillis(final int initialRetryWaitMillis) {
            this.initialRetryWaitMillis = initialRetryWaitMillis;
            return this;
        }

        public Builder withMaxRetryWaitMillis(final int maxRetryWaitMillis) {
            this.maxRetryWaitMillis = maxRetryWaitMillis;
            return this;
        }

        public RetryExecutor build() {
            return new RetryExecutor(this.retryLimit, this.initialRetryWaitMillis, this.maxRetryWaitMillis);
        }

        private int retryLimit;
        private int initialRetryWaitMillis;
        private int maxRetryWaitMillis;
    }

    public static Builder builder() {
        return new Builder();
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
                retryWait = op.adjustRetryWait(exception, retryCount, retryLimit, retryWait);
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

    private static final int DEFAULT_RETRY_LIMIT = 3;
    private static final int DEFAULT_INITIAL_RETRY_WAIT_MILLIS = 500;
    private static final int DEFAULT_MAX_RETRY_WAIT_MILLIS = 30 * 60 * 1000;

    private final int retryLimit;
    private final int initialRetryWait;
    private final int maxRetryWait;
}
