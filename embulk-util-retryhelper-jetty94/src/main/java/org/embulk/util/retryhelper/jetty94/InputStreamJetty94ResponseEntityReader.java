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

package org.embulk.util.retryhelper.jetty94;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;

public class InputStreamJetty94ResponseEntityReader
        implements Jetty94ResponseReader<InputStream>
{
    public InputStreamJetty94ResponseEntityReader(long timeoutMillis)
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
    public final InputStream readResponseContent()
            throws Exception
    {
        return this.listener.getInputStream();
    }

    @Override
    public final String readResponseContentInString()
            throws Exception
    {
        return Util.asString(this.readResponseContent());
    }

    private final InputStreamResponseListener listener;
    private final long timeoutMillis;
}
