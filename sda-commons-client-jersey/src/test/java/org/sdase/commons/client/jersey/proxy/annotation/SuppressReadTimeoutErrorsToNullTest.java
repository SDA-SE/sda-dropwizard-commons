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

class SuppressReadTimeoutErrorsToNullTest {

  @Test
  void shouldSuppressForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new ProcessingException(new SocketTimeoutException());
            });
    assertThat(given.suppressed()).isNull();
  }

  @Test
  void shouldNotSuppressConnectTimeoutForObjectReturnType() {
    ProcessingErrorsSuppressed given =
        createProxy(
            ProcessingErrorsSuppressed.class,
            () -> {
              throw new ProcessingException(new ConnectTimeoutException());
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

    @SuppressReadTimeoutErrorsToNull
    Object suppressed();
  }
}
