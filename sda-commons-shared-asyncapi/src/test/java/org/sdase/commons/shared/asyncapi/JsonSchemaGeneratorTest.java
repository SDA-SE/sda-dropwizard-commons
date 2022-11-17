package org.sdase.commons.shared.asyncapi;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.asyncapi.models.BaseEvent;
import org.sdase.commons.shared.yaml.YamlUtil;

class JsonSchemaGeneratorTest {

  @Test
  void shouldGenerateJsonSchema() throws IOException, URISyntaxException {
    String actual = JsonSchemaGenerator.builder().forClass(BaseEvent.class).generateYaml();
    String expected = TestUtil.readResource("/schema_expected.yaml");

    Map<String, Object> expectedJson =
        YamlUtil.load(expected, new TypeReference<Map<String, Object>>() {});
    Map<String, Object> actualJson =
        YamlUtil.load(actual, new TypeReference<Map<String, Object>>() {});

    assertThat(actualJson).usingRecursiveComparison().isEqualTo(expectedJson);
  }

  @Test
  void shouldSortDefinitions() {
    JsonNode actual = JsonSchemaGenerator.builder().forClass(BaseEvent.class).generate();
    JsonNode definitions = actual.at("/definitions");
    List<String> keys = new ArrayList<>();
    definitions.fieldNames().forEachRemaining(keys::add);
    // usingRecursiveComparison() is unable to compare the order, so we have to do it manually.
    assertThat(keys).isSorted();
  }
}
