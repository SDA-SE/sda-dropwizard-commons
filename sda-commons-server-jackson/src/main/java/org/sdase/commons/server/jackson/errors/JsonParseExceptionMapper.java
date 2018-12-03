package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.core.JsonParseException;
import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Exception mapper for JsonParseException that are thrown if the json is not well formatted
 */
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

   private static final String ERROR_MESSAGE = "Failed to parse json.";
   private static final Logger LOGGER = LoggerFactory.getLogger(JsonParseExceptionMapper.class);

   @Override
   public Response toResponse(JsonParseException exception) {
      LOGGER.error(ERROR_MESSAGE, exception);
      ApiError apiError = new ApiError(ERROR_MESSAGE);
      return Response.status(Response.Status.BAD_REQUEST).type(MediaType.APPLICATION_JSON).entity(apiError).build();
   }
}
