package org.sdase.commons.server.opentelemetry.jaxrs;

import javax.annotation.Nullable;
import javax.ws.rs.core.Request;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;

/**
 * Provides a better name for the server span that is extracted from a jersey {@code
 * ExtendedUriInfo}
 */
public class JerseySpanNameProvider {
  @Nullable
  public String get(Request request) {
    ContainerRequest containerRequest = (ContainerRequest) request;
    ExtendedUriInfo extendedUriInfo = containerRequest.getUriInfo();
    return extendedUriInfo.getMatchedTemplates().stream()
        // 'route', '/route/', 'route/' are normalized to '/route'
        .map(uriTemplate -> JaxrsPathUtil.normalizePath(uriTemplate.getTemplate()))
        .reduce((a, b) -> b + a)
        .orElse(null);
  }
}
