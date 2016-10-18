package org.embulk.util.web_api.client;

import org.embulk.util.web_api.WebApiPluginTask;
import org.embulk.spi.util.RetryExecutor.RetryGiveupException;
import org.embulk.spi.util.RetryExecutor.Retryable;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;

import static com.google.common.base.Throwables.propagate;
import static java.util.Locale.ENGLISH;
import static org.embulk.spi.Exec.getLogger;
import static org.embulk.spi.util.RetryExecutor.retryExecutor;

public class WebApiClient<TASK extends WebApiPluginTask>
        implements AutoCloseable
{
    private final Logger log = getLogger(WebApiClient.class);
    protected final Client client;
    protected final TASK task;

    private WebApiClient(TASK task, Client client)
    {
        this.task = task;
        this.client = client;
    }

    public Client getClient()
    {
        return client;
    }

    public String fetchWithRetry(final RetryableWebApiCall call)
    {
        try {
            return retryExecutor()
                    .withRetryLimit(task.getRetryLimit())
                    .withInitialRetryWait(task.getInitialRetryWait())
                    .withMaxRetryWait(task.getMaxRetryWait())
                    .runInterruptible(new Retryable<String>() {
                        @Override
                        public String call()
                                throws Exception
                        {
                            javax.ws.rs.core.Response response = call.request();

                            if (response.getStatus() / 100 != 2) {
                                throw new WebApplicationException(response);
                            }

                            // TODO read timeout
                            return response.readEntity(String.class);
                        }

                        @Override
                        public boolean isRetryableException(Exception e)
                        {
                            return !call.isNotRetryable(e);
                        }

                        @Override
                        public void onRetry(Exception e, int retryCount, int retryLimit, int retryWait)
                                throws RetryGiveupException
                        {
                            String message = String.format(ENGLISH, "Retrying %d/%d after %d seconds. Message: %s",
                                    retryCount, retryLimit, retryWait / 1000, e.getMessage());
                            if (retryCount % 3 == 0) {
                                log.warn(message, e);
                            }
                            else {
                                log.warn(message);
                            }
                        }

                        @Override
                        public void onGiveup(Exception first, Exception last)
                                throws RetryGiveupException
                        {
                        }
                    });
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw propagate(e);
        }
        catch (RetryGiveupException e) {
            throw propagate(e.getCause());
        }
    }

    @Override
    public void close()
    {
        if (client != null) {
            client.close();
        }
    }

    public static WebApiClient.Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private Client client;

        private Builder()
        {
        }

        public WebApiClient.Builder client(Client client)
        {
            this.client = client;
            return self();
        }

        private WebApiClient.Builder self()
        {
            return this;
        }

        public <TASK extends WebApiPluginTask> WebApiClient build(TASK task)
        {
            return new WebApiClient(task, client);
        }
    }
}
