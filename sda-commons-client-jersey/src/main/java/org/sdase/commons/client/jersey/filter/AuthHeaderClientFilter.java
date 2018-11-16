package org.sdase.commons.client.jersey.filter;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;
import java.util.Optional;

import static org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder.currentRequestContext;

/**
 * A client filter that adds the {@code Authorization} header from the incoming request context to the client request if
 * no {@code Authorization} is set at the client request yet. Will silently not add a header if there is no incoming
 * current request context or the incoming request has no {@code Authorization} header.
 */
public class AuthHeaderClientFilter implements ClientRequestFilter {

   @Override
   public void filter(ClientRequestContext requestContext) {
      if (requestContext.getHeaderString(HttpHeaders.AUTHORIZATION) != null) {
         return;
      }
      findIncomingAuthorization().ifPresent(auth -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, auth));
   }

   private Optional<String> findIncomingAuthorization() {
      return currentRequestContext()
            .map(ContainerRequestContext::getHeaders)
            .map(h -> h.getFirst(HttpHeaders.AUTHORIZATION));
   }
}
