package org.sdase.commons.server.jackson.errors;

import jakarta.validation.ValidationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception Mapper for exceptions that occur during the validation process. This exceptions might
 * occur if the validations are not implemented very well.
 */
@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ValidationException> {

  private static final String ERROR_MESSAGE = "Failed to validate message.";
  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @Override
  public Response toResponse(ValidationException exception) {
    LOGGER.error(ERROR_MESSAGE, exception);
    ApiError apiError = new ApiError(ERROR_MESSAGE);
    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
        .type(MediaType.APPLICATION_JSON)
        .entity(apiError)
        .build();
  }
}
