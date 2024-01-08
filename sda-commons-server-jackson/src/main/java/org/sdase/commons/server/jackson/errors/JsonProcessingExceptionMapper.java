package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exception mapper for JsonProcessingException that are thrown e.g. if the json is not well
 * formatted or mapping to the java object failed
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(JsonProcessingExceptionMapper.class);

  @Override
  public Response toResponse(JsonProcessingException exception) {
    LOGGER.error("Failed to process json", exception);

    String message = getErrorMessage(exception);
    ApiError apiError = new ApiError(message);
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(apiError)
        .build();
  }

  private String getErrorMessage(JsonProcessingException exception) {
    if (exception instanceof JsonMappingException jsonMappingException) {
      return String.format(
          "Failed to process json: Location 'line: %s, column: %s'; FieldName '%s'",
          exception.getLocation().getLineNr(),
          exception.getLocation().getColumnNr(),
          jsonMappingException.getPath().stream()
              .map(JsonMappingException.Reference::getFieldName)
              .collect(Collectors.joining(".")));
    } else {
      return String.format(
          "Failed to process json: Location 'line: %s, column: %s'",
          exception.getLocation().getLineNr(), exception.getLocation().getColumnNr());
    }
  }
}
