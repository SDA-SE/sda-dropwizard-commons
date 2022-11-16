package org.sdase.commons.client.jersey.proxy.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.sdase.commons.client.jersey.proxy.ApiClientInvocationHandler.createProxy;

import java.io.IOException;
import java.net.SocketTimeoutException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.sdase.commons.client.jersey.error.ClientRequestException;

class SuppressConnectTimeoutErrorsToNullTest {

  @Test
  void shouldSuppressForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new ProcessingException(new ConnectTimeoutException());
            });
    assertThat(given.suppressed()).isNull();
  }

  @Test
  void shouldNotSuppressReadTimeoutForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new ProcessingException(new SocketTimeoutException());
            });
    assertThatExceptionOfType(ClientRequestException.class).isThrownBy(given::suppressed);
  }

  @Test
  void shouldNotSuppressProcessingErrorForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new ProcessingException(new IOException());
            });
    assertThatExceptionOfType(ClientRequestException.class).isThrownBy(given::suppressed);
  }

  @Test
  void shouldNotSuppressHttpErrorForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new WebApplicationException(404);
            });
    assertThatExceptionOfType(ClientRequestException.class).isThrownBy(given::suppressed);
  }

  public interface ProcessingErrorsSuppressed {

    @SuppressConnectTimeoutErrorsToNull
    Object suppressed();
  }
}
