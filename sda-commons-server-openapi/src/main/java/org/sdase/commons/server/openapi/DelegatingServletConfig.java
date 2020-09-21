package org.sdase.commons.server.openapi;

import static io.swagger.v3.oas.integration.api.OpenApiContext.OPENAPI_CONTEXT_ID_PREFIX;

import io.swagger.v3.oas.integration.api.OpenApiContext;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * A customized {@link ServletConfig} that proxies the request to an originally provided instance.
 * However, for some init params, it replaces the value with a custom one.
 */
class DelegatingServletConfig implements ServletConfig {
  private final String id;
  private final ServletConfig servletConfig;

  /**
   * Create a new delegating servlet config.
   *
   * @param id the id that should be used to create the context id.
   * @param servletConfig the original config that should be used
   */
  DelegatingServletConfig(String id, ServletConfig servletConfig) {
    this.id = id;
    this.servletConfig = servletConfig;
  }

  @Override
  public String getServletName() {
    return servletConfig.getServletName();
  }

  @Override
  public ServletContext getServletContext() {
    return servletConfig.getServletContext();
  }

  @Override
  public String getInitParameter(String name) {
    // Set the context to be used. If the context doesn't exist, it will be created by {@link
    // GenericOpenApiContextBuilder#buildContext(boolean)} using the provided {@link
    // OpenAPIConfiguration}.
    if (OpenApiContext.OPENAPI_CONTEXT_ID_KEY.equals(name)) {
      return OPENAPI_CONTEXT_ID_PREFIX + id;
    }
    return servletConfig.getInitParameter(name);
  }

  @Override
  public Enumeration<String> getInitParameterNames() {
    return servletConfig.getInitParameterNames();
  }
}
