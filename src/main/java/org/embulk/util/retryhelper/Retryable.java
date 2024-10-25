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

import java.util.concurrent.Callable;

public interface Retryable<T> extends Callable<T> {
    @Override
    T call() throws Exception;

    boolean isRetryableException(Exception exception);

    default int adjustRetryWait(Exception exception, int retryCount, int retryLimit, int retryWait) throws RetryGiveupException {
        return retryWait;
    }

    void onRetry(Exception exception, int retryCount, int retryLimit, int retryWait) throws RetryGiveupException;

    void onGiveup(Exception firstException, Exception lastException) throws RetryGiveupException;
}
