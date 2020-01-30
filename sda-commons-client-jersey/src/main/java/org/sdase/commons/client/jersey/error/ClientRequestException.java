package org.sdase.commons.client.jersey.error;

import java.io.Closeable;
import java.net.SocketTimeoutException;
import java.util.Optional;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.apache.http.conn.ConnectTimeoutException;

/**
 * Exception that wraps any {@link javax.ws.rs.WebApplicationException} that occurred in a Http
 * request to avoid that it is used to delegate the same response to the own caller by default
 * exception mappers.
 */
public class ClientRequestException extends RuntimeException implements Closeable {

  /**
   * @param cause the caught {@link javax.ws.rs.WebApplicationException} that occurred in a Http
   *     request
   */
  public ClientRequestException(Throwable cause) {
    super(cause);
  }

  /** @return if the response returned with a 4xx client error status code */
  public boolean isClientError() {
    return getResponse()
        .map(Response::getStatus)
        .map(status -> status > 399 && status < 500)
        .orElse(false);
  }

  /** @return if the response returned with a 5xx server error status code */
  public boolean isServerError() {
    return getResponse()
        .map(Response::getStatus)
        .map(status -> status > 499 && status < 600)
        .orElse(false);
  }

  /** @return if the request timed out while establishing a connection */
  public boolean isConnectTimeout() {
    return getCause() instanceof ProcessingException
        && getCause().getCause() instanceof ConnectTimeoutException;
  }

  /** @return if the request timed out while reading from an established connection */
  public boolean isReadTimeout() {
    return getCause() instanceof ProcessingException
        && getCause().getCause() instanceof SocketTimeoutException;
  }

  /**
   * @return if the request timed out while establishing a connection or while reading from an
   *     established connection
   */
  public boolean isTimeout() {
    return isConnectTimeout() || isReadTimeout();
  }

  /**
   * @return if the request processing failed. This error usually occurs if the response body could
   *     not be converted to the desired response entity
   */
  public boolean isProcessingError() {
    return getCause() instanceof ProcessingException && !isTimeout();
  }

  /**
   * @return the {@link Response} of the outgoing request if available, otherwise an empty {@link
   *     Optional}
   */
  public Optional<Response> getResponse() {
    return getWebApplicationExceptionCause().map(WebApplicationException::getResponse);
  }

  /**
   * @return the {@link #getCause()} if it is a {@link WebApplicationException}, otherwise an empty
   *     {@link Optional}
   */
  public Optional<WebApplicationException> getWebApplicationExceptionCause() {
    if (getCause() instanceof WebApplicationException) {
      return Optional.of((WebApplicationException) getCause());
    }
    return Optional.empty();
  }

  /** This implementation can be called multiple times and will never throw an exception. */
  @Override
  public void close() {
    try {
      this.getResponse().ifPresent(Response::close);
    } catch (Throwable ignored) { // NOSONAR
    }
  }
}
