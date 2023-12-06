package org.sdase.commons.shared.asyncapi.jsonschema.victools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.params.provider.Arguments.of;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.victools.jsonschema.generator.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SwaggerExampleModuleTest {

  private static final Logger LOG = LoggerFactory.getLogger(SwaggerExampleModuleTest.class);

  SchemaGenerator schemaGenerator =
      new SchemaGenerator(
          new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
              .with(Option.INLINE_ALL_SCHEMAS) // to simplify testing
              .with(new SwaggerExampleModule())
              .build());

  @MethodSource
  @ParameterizedTest
  void shouldProvideExample(String fieldInTest, JsonNode expectedExample) {
    ObjectNode actual = schemaGenerator.generateSchema(TestExample.class);
    LOG.info("Actual: {}", actual);
    if (actual.get("properties") instanceof ObjectNode properties
        && properties.get(fieldInTest) instanceof ObjectNode actualField
        && actualField.get("examples") instanceof ArrayNode examples) {
      assertThat(examples).hasSize(1).containsExactly(expectedExample);
    } else if (expectedExample != null) {
      fail("Could not find /properties/{}/examples in {}", fieldInTest, actual);
    }
  }

  static Stream<Arguments> shouldProvideExample() {
    ObjectMapper objectMapper = new ObjectMapper();
    return Stream.of(
        of("string", new TextNode("foo")),
        of("stringId", new TextNode("51")),
        of("integerId", new IntNode(51)),
        of("strings", objectMapper.createArrayNode().add("foo").add("bar")),
        of("mapUsingFallback", new TextNode("{\"foo\":\"bar")),
        of("objectExample", objectMapper.createObjectNode().put("foo", "bar").put("foobar", "foo")),
        of("noSchemaNoFail", null),
        of("schemaWithoutExampleNoFail", null),
        of("schemaWithBlankExampleNoFail", null));
  }

  @SuppressWarnings("unused")
  static class TestExample {
    @Schema(example = "foo")
    private String string;

    @Schema(example = "51")
    private String stringId;

    @Schema(example = "51")
    private int integerId;

    @Schema(
        example =
            """
            [
              "foo",
              "bar"
            ]
            """)
    private List<String> strings;

    @Schema(example = "{\"foo\":\"bar") // intentional invalid json
    private Map<String, String> mapUsingFallback;

    @Schema(
        example =
            """
            {
              "foo": "bar",
              "foobar": "foo"
            }
            """)
    private Map<String, String> objectExample;

    private String noSchemaNoFail;

    @Schema private String schemaWithoutExampleNoFail;

    @Schema(example = "   ")
    private String schemaWithBlankExampleNoFail;

    public String getString() {
      return string;
    }

    public TestExample setString(String string) {
      this.string = string;
      return this;
    }

    public String getStringId() {
      return stringId;
    }

    public TestExample setStringId(String stringId) {
      this.stringId = stringId;
      return this;
    }

    public int getIntegerId() {
      return integerId;
    }

    public TestExample setIntegerId(int integerId) {
      this.integerId = integerId;
      return this;
    }

    public List<String> getStrings() {
      return strings;
    }

    public TestExample setStrings(List<String> strings) {
      this.strings = strings;
      return this;
    }

    public Map<String, String> getMapUsingFallback() {
      return mapUsingFallback;
    }

    public TestExample setMapUsingFallback(Map<String, String> mapUsingFallback) {
      this.mapUsingFallback = mapUsingFallback;
      return this;
    }

    public Map<String, String> getObjectExample() {
      return objectExample;
    }

    public TestExample setObjectExample(Map<String, String> objectExample) {
      this.objectExample = objectExample;
      return this;
    }

    public String getNoSchemaNoFail() {
      return noSchemaNoFail;
    }

    public TestExample setNoSchemaNoFail(String noSchemaNoFail) {
      this.noSchemaNoFail = noSchemaNoFail;
      return this;
    }

    public String getSchemaWithoutExampleNoFail() {
      return schemaWithoutExampleNoFail;
    }

    public TestExample setSchemaWithoutExampleNoFail(String schemaWithoutExampleNoFail) {
      this.schemaWithoutExampleNoFail = schemaWithoutExampleNoFail;
      return this;
    }

    public String getSchemaWithBlankExampleNoFail() {
      return schemaWithBlankExampleNoFail;
    }

    public TestExample setSchemaWithBlankExampleNoFail(String schemaWithBlankExampleNoFail) {
      this.schemaWithBlankExampleNoFail = schemaWithBlankExampleNoFail;
      return this;
    }
  }
}
