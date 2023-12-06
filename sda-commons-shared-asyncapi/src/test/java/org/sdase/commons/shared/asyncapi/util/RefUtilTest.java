package org.sdase.commons.shared.asyncapi.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefUtilTest {

  @SuppressWarnings("JsonStandardCompliance")
  static final String YAML_OBJECT_WITH_REFS =
      """
      o:
        $ref: "#/components/schemas/object"
        a:
          - $ref: "#/components/schemas/array-0-duplicate"
          - o:
              s: foo
              $ref: "#/components/schemas/array-object"
            $ref: "#/components/schemas/array-1"
          - i: 42
            $ref: "#/components/schemas/array-2"
          - $ref: "#/components/schemas/array-0-duplicate"
      $ref: "#/components/schemas/root"
      """;

  @SuppressWarnings("JsonStandardCompliance")
  static final String YAML_ARRAY_WITH_REFS =
      """
      - $ref: "#/components/schemas/array-0-duplicate"
      - o:
          s: foo
          $ref: "#/components/schemas/array-object"
        $ref: "#/components/schemas/array-1"
      - i: 42
        $ref: "#/components/schemas/array-2"
      - $ref: "#/components/schemas/array-0-duplicate"
      """;

  static final YAMLMapper YAML_MAPPER = new YAMLMapper();

  @Test
  void shouldFindAllRefsRecursivelyInObject() throws JsonProcessingException {
    JsonNode given = YAML_MAPPER.readTree("" /* avoids warning */ + YAML_OBJECT_WITH_REFS);
    List<String> actualFoundRefs = findAllRefs(given);
    assertThat(actualFoundRefs)
        .containsExactlyInAnyOrder(
            "#/components/schemas/object",
            "#/components/schemas/array-0-duplicate",
            "#/components/schemas/array-object",
            "#/components/schemas/array-1",
            "#/components/schemas/array-2",
            "#/components/schemas/array-0-duplicate",
            "#/components/schemas/root");
  }

  @Test
  void shouldReplaceAllRefsRecursivelyInObject() throws JsonProcessingException {
    JsonNode given = YAML_MAPPER.readTree("" /* avoids warning */ + YAML_OBJECT_WITH_REFS);
    RefUtil.updateAllRefsRecursively(
        given,
        textNode ->
            "https://example.com/%s"
                .formatted(textNode.asText().substring(textNode.asText().lastIndexOf("/") + 1)));
    List<String> actualRefs = findAllRefs(given);
    assertThat(actualRefs)
        .containsExactlyInAnyOrder(
            "https://example.com/object",
            "https://example.com/array-0-duplicate",
            "https://example.com/array-object",
            "https://example.com/array-1",
            "https://example.com/array-2",
            "https://example.com/array-0-duplicate",
            "https://example.com/root");
  }

  @Test
  void shouldFindAllRefsRecursivelyInArray() throws JsonProcessingException {
    JsonNode given = new YAMLMapper().readTree("" /* avoids warning */ + YAML_ARRAY_WITH_REFS);
    List<String> actualFoundRefs = findAllRefs(given);
    assertThat(actualFoundRefs)
        .containsExactlyInAnyOrder(
            "#/components/schemas/array-0-duplicate",
            "#/components/schemas/array-object",
            "#/components/schemas/array-1",
            "#/components/schemas/array-2",
            "#/components/schemas/array-0-duplicate");
  }

  @Test
  void shouldReplaceAllRefsRecursivelyInArray() throws JsonProcessingException {
    JsonNode given = YAML_MAPPER.readTree("" /* avoids warning */ + YAML_ARRAY_WITH_REFS);
    RefUtil.updateAllRefsRecursively(
        given,
        textNode ->
            "https://example.com/%s"
                .formatted(textNode.asText().substring(textNode.asText().lastIndexOf("/") + 1)));
    List<String> actualRefs = findAllRefs(given);
    assertThat(actualRefs)
        .containsExactlyInAnyOrder(
            "https://example.com/array-0-duplicate",
            "https://example.com/array-object",
            "https://example.com/array-1",
            "https://example.com/array-2",
            "https://example.com/array-0-duplicate");
  }

  @Test
  void shouldExpandRef() throws JsonProcessingException {
    JsonNode given = YAML_MAPPER.readTree("$ref: '%s'".formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        textNode -> {
          try {
            return YAML_MAPPER.readValue(
                """
                original: '%s'
                someOtherField: some value
                """
                    .formatted(textNode.asText()),
                ObjectNode.class);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
          }
        });
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "some value"));
  }

  @Test
  void shouldExpandRefOverwritingExistingFields() throws JsonProcessingException {
    JsonNode given =
        YAML_MAPPER.readTree(
            """
            $ref: '%s'
            someOtherField: original value
            """
                .formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        textNode -> {
          try {
            return YAML_MAPPER.readValue(
                """
                original: '%s'
                someOtherField: new value
                """
                    .formatted(textNode.asText()),
                ObjectNode.class);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
          }
        });
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "new value"));
  }

  @Test
  void shouldExpandRefOverwritingExistingFieldsAndKeepingUnchanged()
      throws JsonProcessingException {
    JsonNode given =
        YAML_MAPPER.readTree(
            """
            $ref: '%s'
            someOtherField: original value
            notToBeChangedField: should stay
            """
                .formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        textNode -> {
          try {
            return YAML_MAPPER.readValue(
                """
                original: '%s'
                someOtherField: new value
                """
                    .formatted(textNode.asText()),
                ObjectNode.class);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
          }
        });
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("notToBeChangedField", "should stay")
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "new value"));
  }

  @Test
  void shouldExpandRefInArray() throws JsonProcessingException {
    JsonNode given = YAML_MAPPER.readTree("- $ref: '%s'".formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        textNode -> {
          try {
            return YAML_MAPPER.readValue(
                """
                original: '%s'
                someOtherField: some value
                """
                    .formatted(textNode.asText()),
                ObjectNode.class);
          } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
          }
        });
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createArrayNode()
                .add(
                    YAML_MAPPER
                        .createObjectNode()
                        .put("original", "#/components/schemas/Foo")
                        .put("someOtherField", "some value")));
  }

  private static List<String> findAllRefs(JsonNode jsonNode) {
    List<String> actualFoundRefs = new ArrayList<>();
    RefUtil.updateAllRefsRecursively(
        jsonNode,
        textNode -> {
          String value = textNode.asText();
          actualFoundRefs.add(value);
          return value;
        });
    return actualFoundRefs;
  }
}
