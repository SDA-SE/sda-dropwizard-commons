package org.sdase.commons.server.jackson;

import org.assertj.core.api.Assertions;
import org.eclipse.jetty.io.EofException;
import org.junit.Test;
import org.sdase.commons.server.jackson.errors.ApiExceptionMapper;
import org.sdase.commons.server.jackson.errors.EarlyEofExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;

import javax.ws.rs.core.Response;

public class EarlyEofExceptionMapperTest {


   @Test
   public void shouldReturnApiExceptionResponse() {
      EarlyEofExceptionMapper earlyEofExceptionMapper = new EarlyEofExceptionMapper();
      Response resp = earlyEofExceptionMapper.toResponse(new EofException("Eof"));

      Assertions.assertThat(resp.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
      Assertions.assertThat(resp.getEntity()).isInstanceOf(ApiError.class);
      Assertions.assertThat(((ApiError)resp.getEntity()).getTitle()).isEqualTo("EOF Exception encountered - client disconnected during stream processing.");
   }

}
