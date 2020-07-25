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

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public final class DefaultJetty93ClientCreator
        implements Jetty93ClientCreator
{
    public DefaultJetty93ClientCreator(int connectTimeout,
                                       int idleTimeout)
    {
        this(new SslContextFactory(), connectTimeout, idleTimeout, true);
    }

    public DefaultJetty93ClientCreator(int connectTimeout,
                                       int idleTimeout,
                                       boolean tcpNoDelay)
    {
        this(new SslContextFactory(), connectTimeout, idleTimeout, tcpNoDelay);
    }

    public DefaultJetty93ClientCreator(SslContextFactory sslContextFactory,
                                       int connectTimeout,
                                       int idleTimeout)
    {
        this(sslContextFactory, connectTimeout, idleTimeout, true);
    }

    public DefaultJetty93ClientCreator(SslContextFactory sslContextFactory,
                                       int connectTimeout,
                                       int idleTimeout,
                                       boolean tcpNoDelay)
    {
        this.sslContextFactory = sslContextFactory;
        this.connectTimeout = connectTimeout;
        this.idleTimeout = idleTimeout;
        this.tcpNoDelay = tcpNoDelay;
    }

    @Override
    public HttpClient createAndStart()
            throws Exception
    {
        HttpClient client = new HttpClient(this.sslContextFactory);
        client.setConnectTimeout(this.connectTimeout);
        client.setIdleTimeout(this.idleTimeout);
        client.setTCPNoDelay(this.tcpNoDelay);
        client.start();
        return client;
    }

    private final SslContextFactory sslContextFactory;
    private final int connectTimeout;
    private final int idleTimeout;
    private final boolean tcpNoDelay;
}
