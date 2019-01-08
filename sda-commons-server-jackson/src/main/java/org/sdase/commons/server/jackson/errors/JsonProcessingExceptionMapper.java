package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Exception mapper for JsonProcessingException that are thrown e.g. if the json is not well formatted
 * or mapping to the java object failed
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

   private static final String ERROR_MESSAGE = "Failed to process json. Exception '%s'; Location 'line: %s, column: %s'";
   private static final String ERROR_MESSAGE_W_FIELD = "Failed to process json. Exception '%s'; Location 'line: %s, column: %s'; FieldName '%s'";
   private static final Logger LOGGER = LoggerFactory.getLogger(JsonProcessingExceptionMapper.class);

   @Override
   public Response toResponse(JsonProcessingException exception) {
      String message;
      if (exception instanceof JsonMappingException) {
         message = String.format(ERROR_MESSAGE_W_FIELD, exception.getClass(),
               exception.getLocation().getLineNr(),
               exception.getLocation().getColumnNr(),
               ((JsonMappingException)exception).getPath().stream()
                     .map(p -> p.getFieldName())
                     .collect(Collectors.joining(".")),
               Locale.ROOT);
      } else {
         message = String.format(ERROR_MESSAGE, exception.getClass(),
               exception.getLocation().getLineNr(),
               exception.getLocation().getColumnNr(),
               Locale.ROOT);
      }

      LOGGER.error(message);

      ApiError apiError = new ApiError(message);
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(apiError).build();

   }
}
