package org.sdase.commons.server.openapi.filter;

import io.swagger.v3.core.filter.AbstractSpecFilter;
import io.swagger.v3.core.filter.SpecFilter;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.UriInfo;

/**
 * An {@link io.swagger.v3.core.filter.OpenAPISpecFilter} that adds the base URL to {@link
 * OpenAPI#servers(List)}.
 */
@Singleton
public class ServerUrlFilter extends AbstractSpecFilter implements Feature {
  @Context UriInfo uriInfo;

  @Override
  public Optional<OpenAPI> filterOpenAPI(
      OpenAPI openAPI,
      Map<String, List<String>> params,
      Map<String, String> cookies,
      Map<String, List<String>> headers) {

    // Clone the OpenAPI specification to not edit the original one that is reused across requests.
    final OpenAPI clone =
        new SpecFilter().filter(openAPI, new AbstractSpecFilter() {}, null, null, null);

    if (clone.getServers() != null) {
      clone.setServers(new ArrayList<>(clone.getServers()));
    }

    // Add the base URL
    clone.addServersItem(new Server().url(uriInfo.getBaseUri().toString()));

    return Optional.of(clone);
  }

  @Override
  public boolean configure(FeatureContext context) {
    return true;
  }
}
