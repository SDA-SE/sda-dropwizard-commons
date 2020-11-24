package org.sdase.commons.server.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.ObjectMapperFactory;
import org.junit.Test;

public class OpenApiFileHelperTest {

  @Test
  public void normalizeOpenApiYaml() throws JsonProcessingException {
    OpenAPI openAPI = new OpenAPI().addServersItem(new Server().url("http://test"));
    String yamlString = ObjectMapperFactory.createYaml().writeValueAsString(openAPI);

    assertThat(OpenApiFileHelper.normalizeOpenApiYaml(yamlString))
        .doesNotContain("servers")
        .doesNotContain("http://test");
  }
}
