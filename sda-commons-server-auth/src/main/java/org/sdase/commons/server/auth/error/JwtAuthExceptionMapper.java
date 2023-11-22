package org.sdase.commons.server.auth.error;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;

public class JwtAuthExceptionMapper implements ExceptionMapper<JwtAuthException> {
  @Override
  public Response toResponse(JwtAuthException exception) {
    return Response.status(401)
        .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ApiError(exception.getMessage()))
        .build();
  }
}
