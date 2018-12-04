package org.sdase.commons.client.jersey.error;

import org.sdase.commons.shared.api.error.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * Exception mapper that converts {@link ClientRequestException} thrown from an outgoing request to another service into
 * a {@code 500 Internal Server Error} response for our client.
 */
public class ClientRequestExceptionMapper implements ExceptionMapper<ClientRequestException> {

   private static final Logger LOG = LoggerFactory.getLogger(ClientRequestExceptionMapper.class);

   private static final String TITLE_PREFIX = "Request could not be fulfilled: ";
   private static final String UNKNOWN_SUBTITLE = "Unknown error when invoking another service.";
   private static final String STATUS_CODE_SUBTITLE = "Received status '%s' from another service.";
   private static final String READ_TIMEOUT_SUBTITLE = "Read timeout when invoking another service.";
   private static final String CONNECT_TIMEOUT_SUBTITLE = "Connect timeout when invoking another service.";
   private static final String PROCESSING_SUBTITLE = "Processing failed when invoking another service.";

   private static final Response.Status RESPONSE_STATUS_CODE = Response.Status.INTERNAL_SERVER_ERROR;

   @Override
   public Response toResponse(ClientRequestException exception) {
      String title = createTitle(exception);
      LOG.info("Client request error not handled in application: {}", title, exception);
      ApiError error = new ApiError(title, emptyList());
      return Response.status(RESPONSE_STATUS_CODE).type(APPLICATION_JSON).entity(error).build();
   }

   private String createTitle(ClientRequestException clientRequestException) {
      if (clientRequestException.isReadTimeout()) {
         return createTitle(READ_TIMEOUT_SUBTITLE);
      }
      if (clientRequestException.isConnectTimeout()) {
         return createTitle(CONNECT_TIMEOUT_SUBTITLE);
      }
      if (clientRequestException.isProcessingError()) {
         return createTitle(PROCESSING_SUBTITLE);
      }
      return clientRequestException.getWebApplicationExceptionCause()
               .map(WebApplicationException::getResponse)
               .map(this::createTitle)
               .orElse(createUnknownTitle());
   }

   private String createTitle(Response response) {
      return createTitle(format(STATUS_CODE_SUBTITLE, "" + response.getStatus()));
   }

   private String createUnknownTitle() {
      return createTitle(UNKNOWN_SUBTITLE);
   }

   private String createTitle(String subtitle) {
      return TITLE_PREFIX + subtitle;
   }
}
