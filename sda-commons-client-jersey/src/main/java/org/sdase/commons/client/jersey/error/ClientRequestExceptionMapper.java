package org.sdase.commons.client.jersey.error;

import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static java.lang.String.format;
import static java.util.Collections.emptyList;

/**
 * Exception mapper that converts {@link ClientRequestException} thrown from an outgoing request to another service into
 * a {@code 503 Service Unavailable} response for our client.
 */
public class ClientRequestExceptionMapper implements ExceptionMapper<ClientRequestException> {

   private static final Logger LOG = LoggerFactory.getLogger(ClientRequestExceptionMapper.class);

   private static final String TITLE_PREFIX = "Request could not be fulfilled: ";
   private static final String UNKNOWN_SUBTITLE = "Unknown error when invoking another service.";
   private static final String STATUS_CODE_SUBTITLE = "Received status '%s' from another service.";

   private static final Response.Status RESPONSE_STATUS_CODE = Response.Status.SERVICE_UNAVAILABLE;

   @Override
   public Response toResponse(ClientRequestException exception) {
      String title = createTitle(exception);
      LOG.info("Client request error not handled in application: {}", title, exception);
      ApiError error = new ApiError(title, emptyList());
      return Response.status(RESPONSE_STATUS_CODE)
            .type(MediaType.APPLICATION_JSON)
            .entity(error)
            .build();
   }

   private String createTitle(ClientRequestException clientRequestException) {
      if (clientRequestException.getCause() != null) {
         return createTitle(clientRequestException.getCause());
      }
      return TITLE_PREFIX + UNKNOWN_SUBTITLE;
   }

   private String createTitle(WebApplicationException webApplicationException) {
      if (webApplicationException.getResponse() != null) {
         return createTitle(webApplicationException.getResponse());
      }
      if (webApplicationException instanceof ClientErrorException) {
         return TITLE_PREFIX + format(STATUS_CODE_SUBTITLE, "4xx");
      }
      if (webApplicationException instanceof ServerErrorException) {
         return TITLE_PREFIX + format(STATUS_CODE_SUBTITLE, "5xx");
      }
      return TITLE_PREFIX + UNKNOWN_SUBTITLE;
   }

   private String createTitle(Response response) {
      return TITLE_PREFIX + format(STATUS_CODE_SUBTITLE, "" + response.getStatus());
   }
}
