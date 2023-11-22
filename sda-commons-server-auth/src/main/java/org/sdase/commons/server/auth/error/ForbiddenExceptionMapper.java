package org.sdase.commons.server.auth.error;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;

public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
  @Override
  public Response toResponse(ForbiddenException exception) {
    return Response.status(403)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(new ApiError(exception.getMessage()))
        .build();
  }
}
