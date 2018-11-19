package org.sdase.commons.client.jersey.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

import static org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder.currentRequestContext;

/**
 * A client filter that adds the {@code Authorization} header from the incoming request context to the client request if
 * no {@code Authorization} is set at the client request yet. Will silently not add a header if there is no incoming
 * current request context or the incoming request has no {@code Authorization} header.
 */
public class AuthHeaderClientFilter extends AddRequestHeaderFilter {

   public AuthHeaderClientFilter() {
      super(HttpHeaders.AUTHORIZATION, () -> currentRequestContext()
            .map(ContainerRequestContext::getHeaders)
            .map(h -> h.getFirst(HttpHeaders.AUTHORIZATION)));
   }
}
