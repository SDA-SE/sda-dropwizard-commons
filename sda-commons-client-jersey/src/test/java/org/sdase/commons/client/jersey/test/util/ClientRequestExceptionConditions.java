package org.sdase.commons.client.jersey.test.util;

import org.assertj.core.api.Condition;
import org.sdase.commons.client.jersey.error.ClientRequestException;

/**
 * Utility class to create AssertJ test {@link Condition}s for {@link ClientRequestException}s. The
 * created conditions can be used with {@link org.assertj.core.api.ThrowableAssert#is(Condition)} or
 * {@link org.assertj.core.api.ThrowableAssert#has(Condition)}.
 */
public class ClientRequestExceptionConditions {

  private ClientRequestExceptionConditions() {}

  public static Condition<ClientRequestException> clientError() {
    return new Condition<>(ClientRequestException::isClientError, "client error");
  }

  public static Condition<ClientRequestException> serverError() {
    return new Condition<>(ClientRequestException::isServerError, "server error");
  }

  public static Condition<ClientRequestException> timeoutError() {
    return new Condition<>(ClientRequestException::isTimeout, "timeout error");
  }

  public static Condition<ClientRequestException> connectTimeoutError() {
    return new Condition<>(ClientRequestException::isTimeout, "connect timeout error");
  }

  public static Condition<ClientRequestException> readTimeoutError() {
    return new Condition<>(ClientRequestException::isTimeout, "read timeout error");
  }

  public static Condition<ClientRequestException> processingError() {
    return new Condition<>(ClientRequestException::isProcessingError, "processing error");
  }

  public static Condition<ClientRequestException> webApplicationExceptionCause() {
    return new Condition<>(
        (ClientRequestException e) -> e.getWebApplicationExceptionCause().isPresent(),
        "WebApplicationException as cause");
  }
}
