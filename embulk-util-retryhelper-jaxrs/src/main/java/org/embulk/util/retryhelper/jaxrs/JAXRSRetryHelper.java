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

package org.embulk.util.retryhelper.jaxrs;

import java.util.Locale;
import org.embulk.util.retryhelper.Retryable;
import org.embulk.util.retryhelper.RetryExecutor;
import org.embulk.util.retryhelper.RetryGiveupException;
import org.slf4j.LoggerFactory;

public class JAXRSRetryHelper
        implements AutoCloseable
{
    public JAXRSRetryHelper(int maximumRetries,
                            int initialRetryIntervalMillis,
                            int maximumRetryIntervalMillis,
                            JAXRSClientCreator clientCreator)
    {
        this(maximumRetries,
             initialRetryIntervalMillis,
             maximumRetryIntervalMillis,
             clientCreator.create(),
             true,
             LoggerFactory.getLogger(JAXRSRetryHelper.class));
    }

    public JAXRSRetryHelper(int maximumRetries,
                            int initialRetryIntervalMillis,
                            int maximumRetryIntervalMillis,
                            JAXRSClientCreator clientCreator,
                            final org.slf4j.Logger logger)
    {
        this(maximumRetries,
             initialRetryIntervalMillis,
             maximumRetryIntervalMillis,
             clientCreator.create(),
             true,
             logger);
    }

    /**
     * Creates a {@code JAXRSRetryHelper} instance with a ready-made JAX-RS {@code Client} instance.
     *
     * Note that the {@code Client} instance is not automatically closed.
     */
    public static JAXRSRetryHelper createWithReadyMadeClient(int maximumRetries,
                                                             int initialRetryIntervalMillis,
                                                             int maximumRetryIntervalMillis,
                                                             final javax.ws.rs.client.Client client,
                                                             final org.slf4j.Logger logger)
    {
        return new JAXRSRetryHelper(maximumRetries,
                                    initialRetryIntervalMillis,
                                    maximumRetryIntervalMillis,
                                    client,
                                    false,
                                    logger);
    }

    private JAXRSRetryHelper(int maximumRetries,
                             int initialRetryIntervalMillis,
                             int maximumRetryIntervalMillis,
                             final javax.ws.rs.client.Client client,
                             boolean closeAutomatically,
                             final org.slf4j.Logger logger)
    {
        this.maximumRetries = maximumRetries;
        this.initialRetryIntervalMillis = initialRetryIntervalMillis;
        this.maximumRetryIntervalMillis = maximumRetryIntervalMillis;
        this.client = client;;
        this.closeAutomatically = closeAutomatically;
        this.logger = logger;
    }

    public <T> T requestWithRetry(final JAXRSResponseReader<T> responseReader,
                                  final JAXRSSingleRequester singleRequester)
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
                            // |javax.ws.rs.ProcessingException| can be throws
                            // by timeout in connection or reading.
                            javax.ws.rs.core.Response response = singleRequester.requestOnce(client);

                            if (response.getStatus() / 100 != 2) {
                                throw new javax.ws.rs.WebApplicationException(response);
                            }

                            return responseReader.readResponse(response);
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
            throw new RuntimeException(ex.getCause());
        }
    }

    @Override
    public void close()
    {
        if (this.closeAutomatically && this.client != null) {
            this.client.close();
        }
    }

    private final int maximumRetries;
    private final int initialRetryIntervalMillis;
    private final int maximumRetryIntervalMillis;
    private final javax.ws.rs.client.Client client;
    private final org.slf4j.Logger logger;
    private final boolean closeAutomatically;
}
