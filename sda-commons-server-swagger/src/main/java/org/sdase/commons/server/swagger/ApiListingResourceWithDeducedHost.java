package org.sdase.commons.server.swagger;

import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

// Swagger doesn't deduce the host automatically by default. This script makes sure that the swagger
// definition always contains a host field. See https://github.com/swagger-api/swagger-core/issues/2558
public class ApiListingResourceWithDeducedHost extends ApiListingResource {

   @Override
   protected Swagger process(Application app, ServletContext servletContext, ServletConfig sc, HttpHeaders headers,
         UriInfo uriInfo) {
      Swagger swagger = super.process(app, servletContext, sc, headers, uriInfo);
      // Make sure to copy the swagger object to be thread safe (the object is cached internally)
      swagger = copySwagger(swagger);
      swagger.setSchemes(Collections.singletonList(Scheme.forValue(uriInfo.getBaseUri().getScheme())));

      // If the api is located at the default port (80 for http, 433 for https) getPort() == -1
      if (uriInfo.getBaseUri().getPort() == -1) {
         swagger.setHost(uriInfo.getBaseUri().getHost());
      } else {
         swagger.setHost(uriInfo.getBaseUri().getHost() + ":" + uriInfo.getBaseUri().getPort());
      }

      return swagger;
   }

   private Swagger copySwagger(Swagger swagger) {
      Swagger swaggerCopy = new Swagger()
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
}
