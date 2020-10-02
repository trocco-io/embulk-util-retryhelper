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

import java.util.Locale;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponseException;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.embulk.util.retryhelper.Retryable;
import org.embulk.util.retryhelper.RetryExecutor;
import org.embulk.util.retryhelper.RetryGiveupException;
import org.slf4j.LoggerFactory;

public class Jetty93RetryHelper
        implements AutoCloseable
{
    public Jetty93RetryHelper(int maximumRetries,
                              int initialRetryIntervalMillis,
                              int maximumRetryIntervalMillis,
                              Jetty93ClientCreator clientCreator)
    {
        this.maximumRetries = maximumRetries;
        this.initialRetryIntervalMillis = initialRetryIntervalMillis;
        this.maximumRetryIntervalMillis = maximumRetryIntervalMillis;
        try {
            this.clientStarted = clientCreator.createAndStart();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        this.closeAutomatically = true;
        this.logger = LoggerFactory.getLogger(Jetty93RetryHelper.class);
    }

    public Jetty93RetryHelper(int maximumRetries,
                              int initialRetryIntervalMillis,
                              int maximumRetryIntervalMillis,
                              Jetty93ClientCreator clientCreator,
                              final org.slf4j.Logger logger)
    {
        this.maximumRetries = maximumRetries;
        this.initialRetryIntervalMillis = initialRetryIntervalMillis;
        this.maximumRetryIntervalMillis = maximumRetryIntervalMillis;
        try {
            this.clientStarted = clientCreator.createAndStart();
        }
        catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        this.closeAutomatically = true;
        this.logger = logger;
    }

    /**
     * Creates a {@code Jetty93RetryHelper} instance with a ready-made Jetty 9.3 {@code HttpClient} instance.
     *
     * Note that the {@code HttpClient} instance is not automatically closed.
     */
    public static Jetty93RetryHelper createWithReadyMadeClient(int maximumRetries,
                                                               int initialRetryIntervalMillis,
                                                               int maximumRetryIntervalMillis,
                                                               final org.eclipse.jetty.client.HttpClient clientStarted,
                                                               final org.slf4j.Logger logger)
    {
        return new Jetty93RetryHelper(maximumRetries,
                                      initialRetryIntervalMillis,
                                      maximumRetryIntervalMillis,
                                      clientStarted,
                                      false,
                                      logger);
    }

    private Jetty93RetryHelper(int maximumRetries,
                               int initialRetryIntervalMillis,
                               int maximumRetryIntervalMillis,
                               final org.eclipse.jetty.client.HttpClient clientStarted,
                               boolean closeAutomatically,
                               final org.slf4j.Logger logger)
    {
        this.maximumRetries = maximumRetries;
        this.initialRetryIntervalMillis = initialRetryIntervalMillis;
        this.maximumRetryIntervalMillis = maximumRetryIntervalMillis;
        this.logger = logger;
        this.clientStarted = clientStarted;
        this.closeAutomatically = closeAutomatically;
    }

    public <T> T requestWithRetry(final Jetty93ResponseReader<T> responseReader,
                                  final Jetty93SingleRequester singleRequester)
    {
        try {
            return RetryExecutor.builder()
                .withRetryLimit(this.maximumRetries)
                .withInitialRetryWaitMillis(this.initialRetryIntervalMillis)
                .withMaxRetryWaitMillis(this.maximumRetryIntervalMillis)
                .build()
                .runInterruptible(new Retryable<T>() {
                        @Override
                        public T call()
                                throws Exception
                        {
                            Response.Listener listener = responseReader.getListener();
                            singleRequester.requestOnce(clientStarted, listener);
                            Response response = responseReader.getResponse();
                            if (response.getStatus() / 100 != 2) {
                                final String errorResponseBody;
                                try {
                                    errorResponseBody = responseReader.readResponseContentInString();
                                }
                                catch (Exception ex) {
                                    throw new HttpResponseException(
                                        "Response not 2xx: "
                                        + response.getStatus() + " "
                                        + response.getReason() + " "
                                        + "Response body not available by: " + Util.getStackTraceAsString(ex),
                                        response);
                                }
                                throw new HttpResponseException(
                                    "Response not 2xx: "
                                    + response.getStatus() + " "
                                    + response.getReason() + " "
                                    + errorResponseBody,
                                    response);
                            }
                            return responseReader.readResponseContent();
                        }

                        @Override
                        public boolean isRetryableException(Exception exception)
                        {
                            return singleRequester.toRetry(exception);
                        }

                        @Override
                        public void onRetry(Exception exception, int retryCount, int retryLimit, int retryWait)
                                throws RetryGiveupException
                        {
                            String message = String.format(
                                Locale.ENGLISH, "Retrying %d/%d after %d seconds. Message: %s",
                                retryCount, retryLimit, retryWait / 1000, exception.getMessage());
                            if (retryCount % 3 == 0) {
                                logger.warn(message, exception);
                            }
                            else {
                                logger.warn(message);
                            }
                        }

                        @Override
                        public void onGiveup(Exception first, Exception last)
                                throws RetryGiveupException
                        {
                        }
                    });
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            // InterruptedException must not be RuntimeException.
            throw new RuntimeException(ex);
        }
        catch (RetryGiveupException ex) {
            // RetryGiveupException is ExecutionException, which must not be RuntimeException.
            if (ex.getCause() instanceof RuntimeException) {
                throw (RuntimeException) ex.getCause();
            }
            throw new RuntimeException(ex.getCause());
        }
    }

    @Override
    public void close()
    {
        if (this.closeAutomatically && this.clientStarted != null) {
            try {
                if (this.clientStarted.isStarted()) {
                    this.clientStarted.stop();
                }
            }
            catch (Exception ex) {
                throw new RuntimeException(ex);
            }
            finally {
                this.clientStarted.destroy();
            }
        }
    }

    private final int maximumRetries;
    private final int initialRetryIntervalMillis;
    private final int maximumRetryIntervalMillis;
    private final org.eclipse.jetty.client.HttpClient clientStarted;
    private final org.slf4j.Logger logger;
    private final boolean closeAutomatically;
}
