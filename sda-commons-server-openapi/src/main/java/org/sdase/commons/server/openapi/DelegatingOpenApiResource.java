package org.sdase.commons.server.openapi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import javax.inject.Singleton;
import javax.servlet.ServletConfig;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/** An {@link OpenApiResource} that provides a custom {@link ServletConfig}. */
@Singleton
public class DelegatingOpenApiResource extends OpenApiResource implements Feature {
  private final String instanceId;

  @Context ServletConfig servletConfig;

  @Context Application application;

  /**
   * Creates a new resource with an instance id that is used as a key for the registration of the
   * internal caches.
   *
   * @param instanceId the instanceId to be used for the cache keys.
   */
  public DelegatingOpenApiResource(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  public Response getOpenApi(HttpHeaders headers, UriInfo uriInfo, String type) throws Exception {
    return super.getOpenApi(
        headers,
        new DelegatingServletConfig(instanceId, servletConfig),
        application,
        uriInfo,
        type);
  }

  @Override
  public boolean configure(FeatureContext context) {
    return true;
  }
}
