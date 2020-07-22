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

/**
 * JAXRSResponseReader defines a method that reads (understands) JAX-RS {@code Response} to another type.
 *
 * This is prepared so that reading a JAX-RS {@code Response} can be retried in
 * {@code RetryHelper}. If {@code RetryHelper} returns just JAX-RS
 * {@code Response}, developers need to call {@code Response#readEntity} or else
 * by themselves, and retry by themselves as well.
 *
 * Find some predefined {@code ResponseReadable} implementations such as
 * {@code StringJAXRSResponseEntityReader}.
 */
public interface JAXRSResponseReader<T>
{
    T readResponse(javax.ws.rs.core.Response response) throws Exception;
}
