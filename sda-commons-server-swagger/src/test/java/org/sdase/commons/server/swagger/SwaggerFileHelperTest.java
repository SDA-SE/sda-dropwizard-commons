package org.sdase.commons.server.swagger;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.models.Swagger;
import org.junit.Test;
import org.sdase.commons.shared.yaml.YamlUtil;

public class SwaggerFileHelperTest {

  @Test
  public void normalizeSwaggerYaml() throws JsonProcessingException {
    Swagger swagger = new Swagger().host("http://test");
    String yamlString = YamlUtil.writeValueAsString(swagger);

    assertThat(SwaggerFileHelper.normalizeSwaggerYaml(yamlString))
        .doesNotContain("servers")
        .doesNotContain("http://test");
  }
}
