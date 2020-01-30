package org.sdase.commons.server.jackson;

import javax.ws.rs.core.Response;
import org.assertj.core.api.Assertions;
import org.eclipse.jetty.io.EofException;
import org.junit.Assert;
import org.junit.Test;
import org.sdase.commons.server.jackson.errors.EarlyEofExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiException;

/*
 * Test for exception mapper. Most of the exception mapper are tested within integration tests
 */
public class ExceptionMapperTest {

  @Test
  public void shouldReturnApiExceptionResponse() {
    EarlyEofExceptionMapper earlyEofExceptionMapper = new EarlyEofExceptionMapper();
    Response resp = earlyEofExceptionMapper.toResponse(new EofException("Eof"));

    Assertions.assertThat(resp.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    Assertions.assertThat(resp.getEntity()).isInstanceOf(ApiError.class);
    Assertions.assertThat(((ApiError) resp.getEntity()).getTitle())
        .isEqualTo("EOF Exception encountered - client disconnected during stream processing.");
  }

  @Test
  public void shouldReturnApiExceptionWithCause() {
    Assert.assertNotNull(ApiException.builder().httpCode(400).title("Error").build());
    Assert.assertNotNull(
        ApiException.builder()
            .httpCode(400)
            .title("Error")
            .cause(new IllegalStateException("exception"))
            .build());
  }

  @Test(expected = IllegalStateException.class)
  public void shouldReturnErrorForNonExceptionStatusCode() {
    ApiException.builder().httpCode(200).title("Error").build();
  }
}
