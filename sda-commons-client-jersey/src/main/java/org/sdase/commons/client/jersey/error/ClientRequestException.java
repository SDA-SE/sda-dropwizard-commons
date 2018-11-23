package org.sdase.commons.client.jersey.error;

import javax.ws.rs.WebApplicationException;

/**
 * Exception that wraps any {@link javax.ws.rs.WebApplicationException} that occurred in a Http request to avoid that
 * it is used to delegate the same response to the own caller by default exception mappers.
 */
public class ClientRequestException extends RuntimeException {

   private final WebApplicationException cause;

   /**
    * @param cause the caught {@link javax.ws.rs.WebApplicationException} that occurred in a Http request
    */
   public ClientRequestException(WebApplicationException cause) {
      super(cause);
      this.cause = cause;
   }

   @Override
   public WebApplicationException getCause() {
      return cause;
   }
}
