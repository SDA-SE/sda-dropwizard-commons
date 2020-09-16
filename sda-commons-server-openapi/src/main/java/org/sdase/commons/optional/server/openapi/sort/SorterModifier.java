package org.sdase.commons.optional.server.openapi.sort;

import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;

/** A {@link ReaderListener} that runs the {@link OpenAPISorter}. */
@OpenAPIDefinition
public class SorterModifier implements ReaderListener {

  @Override
  public void beforeScan(OpenApiReader reader, OpenAPI openAPI) {
    // nothing to do here
  }

  @Override
  public void afterScan(OpenApiReader reader, OpenAPI openAPI) {
    OpenAPISorter.sort(openAPI);
  }
}
