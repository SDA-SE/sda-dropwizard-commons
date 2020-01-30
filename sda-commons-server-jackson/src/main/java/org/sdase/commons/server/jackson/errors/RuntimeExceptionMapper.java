package org.sdase.commons.server.jackson.errors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception Mapper for {@link RuntimeException} that should be used as last resort to avoid default
 * error messages.
 */
public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  private static final String ERROR_MESSAGE = "An exception occurred.";
  private static final Logger LOGGER = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

  @Override
  public Response toResponse(RuntimeException e) {
    LOGGER.error(ERROR_MESSAGE, e);
    return Response.status(500)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ApiError(ERROR_MESSAGE))
        .build();
  }
}
