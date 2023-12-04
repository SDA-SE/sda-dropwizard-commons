package org.sdase.commons.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.core.Response;
import org.eclipse.jetty.io.EofException;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.jackson.errors.EarlyEofExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiException;

/*
 * Test for exception mapper. Most of the exception mapper are tested within integration tests
 */
class ExceptionMapperTest {

  @Test
  void shouldReturnApiExceptionResponse() {
    EarlyEofExceptionMapper earlyEofExceptionMapper = new EarlyEofExceptionMapper();
    try (Response resp = earlyEofExceptionMapper.toResponse(new EofException("Eof"))) {

      assertThat(resp.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
      assertThat(resp.getEntity()).isInstanceOf(ApiError.class);
      assertThat(((ApiError) resp.getEntity()).getTitle())
          .isEqualTo("EOF Exception encountered - client disconnected during stream processing.");
    }
  }

  @Test
  void shouldReturnApiExceptionWithCause() {
    assertNotNull(ApiException.builder().httpCode(400).title("Error").build());
    assertNotNull(
        ApiException.builder()
            .httpCode(400)
            .title("Error")
            .cause(new IllegalStateException("exception"))
            .build());
  }

  @Test
  void shouldReturnErrorForNonExceptionStatusCode() {
    var codeBuilder = ApiException.builder();
    assertThrows(IllegalStateException.class, () -> codeBuilder.httpCode(200));
  }
}
