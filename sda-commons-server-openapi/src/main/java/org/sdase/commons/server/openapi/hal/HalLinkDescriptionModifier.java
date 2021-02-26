package org.sdase.commons.server.openapi.hal;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

/**
 * An {@link ReaderListener} that provides a description for the {@link HALLink} class that is not
 * annotated and lacks a correct description.
 */
@OpenAPIDefinition
@SuppressWarnings("java:S3740") // ignore "Raw types should not be used" introduced by swagger-core
public class HalLinkDescriptionModifier implements ReaderListener {

  @Override
  public void beforeScan(OpenApiReader reader, OpenAPI openAPI) {
    // nothing to do
  }

  @Override
  public void afterScan(OpenApiReader reader, OpenAPI openAPI) {
    if (openAPI.getComponents() != null
        && openAPI.getComponents().getSchemas() != null
        && openAPI.getComponents().getSchemas().containsKey(HALLink.class.getSimpleName())) {
      Schema schema =
          ModelConverters.getInstance().read(HALLink.class).get(HALLink.class.getSimpleName());
      schema.setDescription("Representation of a link as defined in HAL");
      openAPI.getComponents().getSchemas().put(HALLink.class.getSimpleName(), schema);
    }
  }
}
