package org.embulk.util.retryhelper.jetty93;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;

public class StringJetty93ResponseEntityReader
        implements Jetty93ResponseReader<String>
{
    public StringJetty93ResponseEntityReader(long timeoutMillis)
    {
        this.listener = new InputStreamResponseListener();
        this.timeoutMillis = timeoutMillis;
    }

    @Override
    public final Response.Listener getListener()
    {
        return this.listener;
    }

    @Override
    public final Response getResponse()
            throws Exception
    {
        return this.listener.get(this.timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public final String readResponseContent()
            throws Exception
    {
        return Util.asString(this.listener.getInputStream());
    }

    @Override
    public final String readResponseContentInString()
            throws Exception
    {
        return this.readResponseContent();
    }

    private final InputStreamResponseListener listener;
    private final long timeoutMillis;
}
