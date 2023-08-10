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

import org.eclipse.jetty.client.api.Response;

/**
 * Jetty94ResponseReader defines methods that read (understand) Jetty 9.4's response through {@code Listener}s.
 *
 * Find some predefined {@code Jetty94ResponseReader}s such as {@code StringJetty94ResponseEntityReader}.
 */
public interface Jetty94ResponseReader<T>
{
    Response.Listener getListener();
    Response getResponse() throws Exception;
    T readResponseContent() throws Exception;
    String readResponseContentInString() throws Exception;
}
