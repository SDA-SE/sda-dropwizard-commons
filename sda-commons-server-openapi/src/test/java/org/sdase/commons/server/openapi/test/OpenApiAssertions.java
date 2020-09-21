package org.sdase.commons.server.openapi.test;

import static org.assertj.core.api.Assertions.fail;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import javax.ws.rs.core.Response;

public final class OpenApiAssertions {

  public static void assertValid(Response response) {
    String content = response.readEntity(String.class);

    assertValid(content);
  }

  public static void assertValid(String content) {
    SwaggerParseResult result = new OpenAPIV3Parser().readContents(content);

    if (result.getOpenAPI() == null || !result.getMessages().isEmpty()) {
      fail("Invalid OpenApi 3.0 definition: " + String.join("; ", result.getMessages()));
    }
  }

  private OpenApiAssertions() {
    // prevent instantiation
  }
}
