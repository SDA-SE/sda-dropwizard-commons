package org.sdase.commons.server.jackson.errors;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.jetty.io.EofException;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception mapper if client disconnects unexpectedly. This mapper replaces {@link
 * io.dropwizard.jersey.errors.EarlyEofExceptionMapper} to return the custom {@link ApiError}.
 */
@Provider
public class EarlyEofExceptionMapper implements ExceptionMapper<EofException> {

  private static final String ERROR_MESSAGE =
      "EOF Exception encountered - client disconnected during stream processing.";
  private static final Logger LOGGER = LoggerFactory.getLogger(EarlyEofExceptionMapper.class);

  @Override
  public Response toResponse(EofException exception) {
    LOGGER.error(ERROR_MESSAGE, exception);
    ApiError apiError = new ApiError(ERROR_MESSAGE);
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(apiError)
        .build();
  }
}
