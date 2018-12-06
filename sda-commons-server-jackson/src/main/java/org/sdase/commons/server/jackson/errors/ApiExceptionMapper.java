package org.sdase.commons.server.jackson.errors;


import org.sdase.commons.shared.api.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Exception Mapper for {@link ApiException} that should be used within the business logic
 * to inform the clients about problems during processing
 */
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

   private static final String ERROR_MESSAGE = "Api Exception thrown during request processing";
   private static final Logger LOGGER = LoggerFactory.getLogger(ApiExceptionMapper.class);

   @Override
   public Response toResponse(ApiException e) {
      LOGGER.error(ERROR_MESSAGE, e);
      return Response.status(e.getHttpCode()).type(MediaType.APPLICATION_JSON).entity(e.getDTO()).build();
   }

}
