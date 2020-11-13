package org.sdase.commons.client.jersey.filter;

import static org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder.currentRequestContext;

import java.util.Optional;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.HttpHeaders;

/**
 * A client filter that adds the {@code Authorization} header from the incoming request context to
 * the client request if no {@code Authorization} is set at the client request yet. Will silently
 * not add a header if there is no incoming current request context or the incoming request has no
 * {@code Authorization} header.
 */
public class AuthHeaderClientFilter implements AddRequestHeaderFilter {

  @Override
  public String getHeaderName() {
    return HttpHeaders.AUTHORIZATION;
  }

  @Override
  public Optional<String> getHeaderValue() {
    final Optional<ContainerRequestContext> containerRequestContext = currentRequestContext();
    return containerRequestContext
        .map(ContainerRequestContext::getHeaders)
        .map(h -> h.getFirst(HttpHeaders.AUTHORIZATION));
  }
}
