package org.sdase.commons.server.jackson.errors;


import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Exception Mapper for {@link ApiException} that should be used within the business logic
 * to inform the clients about problems during processing
 */
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

   @Override
   public Response toResponse(ApiException e) {
      return Response.status(e.getHttpCode()).type(MediaType.APPLICATION_JSON).entity(e.getDTO()).build();
   }

}
