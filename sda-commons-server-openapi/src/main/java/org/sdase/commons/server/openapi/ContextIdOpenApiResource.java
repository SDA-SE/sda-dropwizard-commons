package org.sdase.commons.server.openapi;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import javax.servlet.ServletConfig;

/** An {@link OpenApiResource} that sets a custom context id. */
public class ContextIdOpenApiResource extends OpenApiResource {
  private String contextId;

  @Override
  protected String getContextId(ServletConfig config) {
    if (contextId != null) {
      return contextId;
    }

    return super.getContextId(config);
  }

  /**
   * Set a custom context id for the {@link io.swagger.v3.oas.integration.api.OpenApiContext} to use
   * different cache keys for the OpenAPI specifications.
   *
   * @param contextId the context id to use
   * @return this instance
   */
  public OpenApiResource contextId(String contextId) {
    this.contextId = contextId;
    return this;
  }
}
