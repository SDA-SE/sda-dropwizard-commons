package org.sdase.commons.server.errorhandling.rest.api;

import org.sdase.commons.server.errorhandling.rest.model.RequestObject;
import org.sdase.commons.server.errorhandling.rest.model.ResponseObject;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiException;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

/**
 * Implementation of endpoint to provide exceptions
 */
public class ErrorHandlingExampleEndpoint implements ErrorHandlingExampleService {

   @Override
   public ResponseObject notFoundException() {
      throw new NotFoundException("Example always throws NotFoundException");
   }

   @Override
   public Response errorResponse() {
      ApiError apiError = new ApiError("ApiError thrown in code to be used in response");
      return Response.serverError().entity(apiError).build();
   }

   @Override
   public Response apiException() {
      throw ApiException.builder().httpCode(422).title("Semantic exception").build();
   }

   @Override
   public Response validation(RequestObject object) {
      return Response.ok().build();
   }

}
