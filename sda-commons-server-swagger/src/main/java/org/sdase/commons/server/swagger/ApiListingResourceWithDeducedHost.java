package org.sdase.commons.server.swagger;

import static io.swagger.jaxrs.config.SwaggerContextService.*;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

// Swagger doesn't deduce the host automatically by default. This script makes sure that the swagger
// definition always contains a host field. See
// https://github.com/swagger-api/swagger-core/issues/2558
public class ApiListingResourceWithDeducedHost extends ApiListingResource {
  private String instanceId;

  /**
   * Creates a new resource with an instance id that is used as a key for the registration of the
   * swagger internal caches.
   *
   * @param instanceId the instanceId to be used for the cache keys.
   */
  ApiListingResourceWithDeducedHost(String instanceId) {
    this.instanceId = instanceId;
  }

  @Override
  protected Swagger process(
      Application app,
      ServletContext servletContext,
      ServletConfig sc,
      HttpHeaders headers,
      UriInfo uriInfo) {
    Swagger swagger =
        super.process(
            app, servletContext, new DelegatingServletConfig(sc, instanceId), headers, uriInfo);
    // Make sure to copy the swagger object to be thread safe (the object is cached internally)
    swagger = copySwagger(swagger);
    swagger.setSchemes(
        Collections.singletonList(Scheme.forValue(uriInfo.getBaseUri().getScheme())));

    // If the api is located at the default port (80 for http, 433 for https) getPort() == -1
    if (uriInfo.getBaseUri().getPort() == -1) {
      swagger.setHost(uriInfo.getBaseUri().getHost());
    } else {
      swagger.setHost(uriInfo.getBaseUri().getHost() + ":" + uriInfo.getBaseUri().getPort());
    }

    return swagger;
  }

  private Swagger copySwagger(Swagger swagger) {
    Swagger swaggerCopy =
        new Swagger()
            .info(swagger.getInfo())
            .host(swagger.getHost())
            .basePath(swagger.getBasePath())
            .tags(swagger.getTags())
            .schemes(swagger.getSchemes())
            .consumes(swagger.getConsumes())
            .produces(swagger.getProduces())
            .paths(swagger.getPaths())
            .responses(swagger.getResponses())
            .externalDocs(swagger.getExternalDocs())
            .vendorExtensions(swagger.getVendorExtensions());
    swaggerCopy.setSecurityDefinitions(swagger.getSecurityDefinitions());
    swaggerCopy.setSecurity(swagger.getSecurity());
    swaggerCopy.setDefinitions(swagger.getDefinitions());
    swaggerCopy.setParameters(swagger.getParameters());

    return swaggerCopy;
  }

  /**
   * A customized {@link ServletConfig} that proxies the request to an originally provided instance.
   * However, for some init params, it replaces the value with a custom one.
   */
  private static class DelegatingServletConfig implements ServletConfig {
    private final ServletConfig servletConfig;
    private final String id;

    /**
     * Create a new delegating servlet confit.
     *
     * @param servletConfig the original config that should be used
     * @param id the id that should be used to create the config, context, and scanner id.
     */
    DelegatingServletConfig(ServletConfig servletConfig, String id) {
      this.servletConfig = servletConfig;
      this.id = id;
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
      // Set the config, context, and scanner ids to be used. These must be
      // the same as in {@link SwaggerBundle}.
      switch (name) {
        case CONFIG_ID_KEY:
          return CONFIG_ID_PREFIX + id;
        case CONTEXT_ID_KEY:
          return CONTEXT_ID_KEY + "." + id;
        case SCANNER_ID_KEY:
          return SCANNER_ID_PREFIX + id;
        default:
          return servletConfig.getInitParameter(name);
      }
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
      return servletConfig.getInitParameterNames();
    }
  }
}
