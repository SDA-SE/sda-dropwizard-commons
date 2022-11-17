package org.sdase.commons.shared.asyncapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.asyncapi.AsyncApiGenerator.SchemaBuilder;
import org.sdase.commons.shared.asyncapi.models.BaseEvent;
import org.sdase.commons.shared.yaml.YamlUtil;

class AsyncApiGeneratorTest {

  @Test
  void shouldGenerateAsyncApi() throws IOException, URISyntaxException {
    String actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi_template.yaml"))
            .withSchema("./schema.json", BaseEvent.class)
            .generateYaml();
    String expected = TestUtil.readResource("/asyncapi_expected.yaml");

    Map<String, Object> expectedJson =
        YamlUtil.load(expected, new TypeReference<Map<String, Object>>() {});
    Map<String, Object> actualJson =
        YamlUtil.load(actual, new TypeReference<Map<String, Object>>() {});

    assertThat(actualJson).usingRecursiveComparison().isEqualTo(expectedJson);
  }

  @Test
  void shouldNotGenerateAsyncApi() {
    final SchemaBuilder schemaBuilder =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi_template.yaml"))
            .withSchema("BAD_PLACEHOLDER", BaseEvent.class);
    assertThatCode(schemaBuilder::generateYaml).isInstanceOf(UnknownSchemaException.class);
  }

  @Test
  void shouldSortSchemas() {
    JsonNode actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource("/asyncapi_template.yaml"))
            .withSchema("./schema.json", BaseEvent.class)
            .generate();
    JsonNode schemas = actual.at("/components/schemas");
    List<String> keys = new ArrayList<>();
    schemas.fieldNames().forEachRemaining(keys::add);
    // usingRecursiveComparison() is unable to compare the order, so we have to do it manually.
    assertThat(keys).isSorted();
  }
}
