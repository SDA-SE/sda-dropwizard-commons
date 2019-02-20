package org.sdase.commons.server.auth.error;

import org.sdase.commons.shared.api.error.ApiError;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

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
