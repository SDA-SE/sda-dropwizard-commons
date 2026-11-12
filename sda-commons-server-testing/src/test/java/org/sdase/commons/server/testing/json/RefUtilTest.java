package org.sdase.commons.server.testing.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

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
  void shouldFindAllRefsRecursivelyInObject() {
    JsonNode given = YAML_MAPPER.readTree(YAML_OBJECT_WITH_REFS);
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
  void shouldReplaceAllRefsRecursivelyInObject() {
    JsonNode given = YAML_MAPPER.readTree(YAML_OBJECT_WITH_REFS);
    RefUtil.updateAllRefsRecursively(
        given,
        stringNode ->
            "https://example.com/%s"
                .formatted(
                    stringNode.asString().substring(stringNode.asString().lastIndexOf("/") + 1)));
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
  void shouldFindAllRefsRecursivelyInArray() {
    JsonNode given = new YAMLMapper().readTree(YAML_ARRAY_WITH_REFS);
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
  void shouldReplaceAllRefsRecursivelyInArray() {
    JsonNode given = YAML_MAPPER.readTree(YAML_ARRAY_WITH_REFS);
    RefUtil.updateAllRefsRecursively(
        given,
        stringNode ->
            "https://example.com/%s"
                .formatted(
                    stringNode.asString().substring(stringNode.asString().lastIndexOf("/") + 1)));
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
  void shouldExpandRef() {
    JsonNode given = YAML_MAPPER.readTree("$ref: '%s'".formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        stringNode ->
            YAML_MAPPER.readValue(
                """
              original: '%s'
              someOtherField: some value
              """
                    .formatted(stringNode.asString()),
                ObjectNode.class));
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "some value"));
  }

  @Test
  void shouldExpandRefOverwritingExistingFields() {
    JsonNode given =
        YAML_MAPPER.readTree(
            """
            $ref: '%s'
            someOtherField: original value
            """
                .formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        stringNode ->
            YAML_MAPPER.readValue(
                """
            original: '%s'
            someOtherField: new value
            """
                    .formatted(stringNode.asString()),
                ObjectNode.class));
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "new value"));
  }

  @Test
  void shouldExpandRefOverwritingExistingFieldsAndKeepingUnchanged() {
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
        stringNode ->
            YAML_MAPPER.readValue(
                """
            original: '%s'
            someOtherField: new value
            """
                    .formatted(stringNode.asString()),
                ObjectNode.class));
    assertThat(given)
        .isEqualTo(
            YAML_MAPPER
                .createObjectNode()
                .put("notToBeChangedField", "should stay")
                .put("original", "#/components/schemas/Foo")
                .put("someOtherField", "new value"));
  }

  @Test
  void shouldExpandRefInArray() {
    JsonNode given = YAML_MAPPER.readTree("- $ref: '%s'".formatted("#/components/schemas/Foo"));
    RefUtil.mergeAllObjectsWithRefsRecursively(
        given,
        stringNode ->
            YAML_MAPPER.readValue(
                """
            original: '%s'
            someOtherField: some value
            """
                    .formatted(stringNode.asString()),
                ObjectNode.class));
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
        stringNode -> {
          String value = stringNode.asString();
          actualFoundRefs.add(value);
          return value;
        });
    return actualFoundRefs;
  }
}
