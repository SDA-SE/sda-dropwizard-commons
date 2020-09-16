/*
 * Copyright (c) 2017 Open API Tools
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * Based on https://github.com/openapi-tools/swagger-hal/blob/05c00c9d5734731a1d08b4b43e0156279629a08d/src/test/java/io/openapitools/hal/example/model/TransactionsRepresentation.java
 */
package org.sdase.commons.server.openapi.apps.hal.model;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource;
import io.openapitools.jackson.dataformat.hal.annotation.Link;
import io.openapitools.jackson.dataformat.hal.annotation.Resource;
import java.util.Collection;
import java.util.Collections;

/** Represents a set of transactions as returned by the REST service. */
@Resource
public class TransactionsRepresentation {
  @EmbeddedResource("transactions")
  private Collection<TransactionRepresentation> transactions;

  @Link private HALLink self;

  public Collection<TransactionRepresentation> getTransactions() {
    return Collections.unmodifiableCollection(transactions);
  }

  public HALLink getSelf() {
    return self;
  }
}
