package org.sdase.commons.client.jersey.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link ClientRequestFilter} that adds a Http header if that header is not set yet and the implementation
 * provides a value for the header.
 */
public class AddRequestHeaderFilter implements ClientRequestFilter {

   private String headerName;

   private Supplier<Optional<String>> headerValueSupplier;

   /**
    * @param headerName the Http header to set, e.g. {@code Authorization}
    * @param headerValueSupplier a supplier of the header value that should be set, if the header is not set yet
    */
   public AddRequestHeaderFilter(String headerName, Supplier<Optional<String>> headerValueSupplier) {
      this.headerName = headerName;
      this.headerValueSupplier = headerValueSupplier;
   }

   @Override
   public void filter(ClientRequestContext requestContext) {
      if (requestContext.getHeaderString(headerName) != null) {
         return;
      }
      headerValueSupplier.get().ifPresent(v -> requestContext.getHeaders().add(headerName, v));
   }
}
