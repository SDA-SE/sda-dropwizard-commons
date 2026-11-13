package org.sdase.commons.server.testing.jsonschema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Type;
import java.util.List;
import org.junit.jupiter.api.Test;

class JsonSchemaGeneratorTest {

  private final JsonSchemaGenerator generator = JsonSchemaGenerator.defaultGenerator();

  @Test
  void preservesFieldOrderWithoutAnnotation() {
    ObjectNode schema = generator.generate(UnorderedFields.class);

    assertThat(schema.at("/$defs/UnorderedFields/properties").fieldNames())
        .toIterable()
        .containsExactly("zebra", "alpha", "middle");
  }

  @Test
  void honorsExplicitPropertyOrderAndPreservesRemainingFieldOrder() {
    ObjectNode schema = generator.generate(ExplicitlyOrderedFields.class);

    assertThat(schema.at("/$defs/ExplicitlyOrderedFields/properties").fieldNames())
        .toIterable()
        .containsExactly("middle", "zebra", "alpha");
  }

  @Test
  void honorsAlphabeticalPropertyOrder() {
    ObjectNode schema = generator.generate(AlphabeticallyOrderedFields.class);

    assertThat(schema.at("/$defs/AlphabeticallyOrderedFields/properties").fieldNames())
        .toIterable()
        .containsExactly("alpha", "middle", "zebra");
  }

  @Test
  void generatesStandaloneDraft202012SchemaWithValidationAndExamples() {
    ObjectNode schema = generator.generate(ContractBundle.class);

    assertThat(schema.path("$schema").asText())
        .isEqualTo("https://json-schema.org/draft/2020-12/schema");
    assertThat(schema.at("/$defs/ContractBundle/properties/id/minLength").asInt()).isEqualTo(1);
    assertThat(schema.at("/$defs/ContractBundle/properties/id/pattern").asText())
        .isEqualTo("^.*\\S+.*$");
    assertThat(schema.at("/$defs/ContractBundle/properties/id/examples/0").asText())
        .isEqualTo("contract-42");
    assertInternalReferencesResolve(schema);
  }

  @Test
  void addsSchemaIdToStandaloneSchema() {
    String schemaId = "https://schemas.example.com/contracts/contract-bundle/1.2.0";

    ObjectNode schema = generator.generate(ContractBundle.class, schemaId);

    assertThat(schema.path("$id").asText()).isEqualTo(schemaId);
    assertThat(schema.findValues("$id")).containsExactly(schema.path("$id"));
    assertInternalReferencesResolve(schema);
  }

  @Test
  void generatesCatalogWithObjectAndListRoots() {
    Type listOfContractBundles = new TypeReference<List<ContractBundle>>() {}.getType();

    ObjectNode catalog = generator.generate(List.of(ContractBundle.class, listOfContractBundles));

    assertThat(catalog.path("$schema").asText())
        .isEqualTo("https://json-schema.org/draft/2020-12/schema");
    assertThat(catalog.at("/oneOf/0/$ref").asText()).isEqualTo("#/$defs/ContractBundle");
    assertThat(catalog.at("/$defs/ContractBundle/type").asText()).isEqualTo("object");
    assertThat(catalog.at("/oneOf/1/type").asText()).isEqualTo("array");
    assertThat(catalog.at("/oneOf/1/items/$ref").asText()).isEqualTo("#/$defs/ContractBundle");
    assertInternalReferencesResolve(catalog);
  }

  @Test
  void addsSchemaIdToCatalog() {
    String schemaId = "https://schemas.example.com/contracts/contract-bundle/1.3.0";

    ObjectNode catalog =
        generator.generate(List.of(ContractBundle.class, UnorderedFields.class), schemaId);

    assertThat(catalog.path("$id").asText()).isEqualTo(schemaId);
    assertThat(catalog.fieldNames()).toIterable().startsWith("$schema", "$id", "oneOf");
    assertThat(catalog.findValues("$id")).containsExactly(catalog.path("$id"));
    assertInternalReferencesResolve(catalog);
  }

  @Test
  void generatesStandaloneSchemaForOneDistinctRoot() {
    ObjectNode schema = generator.generate(List.of(ContractBundle.class, ContractBundle.class));

    assertThat(schema.path("oneOf").isMissingNode()).isTrue();
    assertThat(schema.path("$ref").asText()).isEqualTo("#/$defs/ContractBundle");
    assertInternalReferencesResolve(schema);
  }

  @Test
  void addsSchemaIdToSingleRootGeneratedFromIterable() {
    String schemaId = "https://schemas.example.com/contracts/contract-bundle/1.4.0";

    ObjectNode schema = generator.generate(List.of(ContractBundle.class), schemaId);

    assertThat(schema.path("$id").asText()).isEqualTo(schemaId);
    assertThat(schema.findValues("$id")).containsExactly(schema.path("$id"));
    assertInternalReferencesResolve(schema);
  }

  @Test
  void keepsSameSimpleNameModelsAsSeparateDefinitions() {
    ObjectNode schema = generator.generate(DuplicateNames.class);

    String firstReference = schema.at("/$defs/DuplicateNames/properties/first/$ref").asText();
    String secondReference = schema.at("/$defs/DuplicateNames/properties/second/$ref").asText();

    assertThat(firstReference).startsWith("#/$defs/").isNotEqualTo(secondReference);
    assertThat(schema.at(firstReference.substring(1) + "/properties/first").isMissingNode())
        .isFalse();
    assertThat(schema.at(secondReference.substring(1) + "/properties/second").isMissingNode())
        .isFalse();
    assertThat(generator.generate(DuplicateNames.class)).isEqualTo(schema);
    assertInternalReferencesResolve(schema);
  }

  @Test
  void rejectsNullRootsWithJsonSchemaGenerationException() {
    assertThatThrownBy(() -> generator.generate((Type) null))
        .isInstanceOf(JsonSchemaGenerationException.class)
        .hasMessage("JSON Schema generation input must not be null");
  }

  private static void assertInternalReferencesResolve(ObjectNode schema) {
    schema
        .findValues("$ref")
        .forEach(
            reference -> {
              String value = reference.asText();
              if (value.startsWith("#/")) {
                assertThat(schema.at(value.substring(1))).isNotNull();
                assertThat(schema.at(value.substring(1)).isMissingNode()).isFalse();
              }
            });
  }

  record UnorderedFields(String zebra, String alpha, String middle) {}

  @JsonPropertyOrder({"middle"})
  record ExplicitlyOrderedFields(String zebra, String alpha, String middle) {}

  @JsonPropertyOrder(alphabetic = true)
  record AlphabeticallyOrderedFields(String zebra, String alpha, String middle) {}

  record ContractBundle(@NotBlank @Schema(example = "contract-42") String id) {}

  record DuplicateNames(First.Status first, Second.Status second) {}

  static class First {
    record Status(String first) {}
  }

  static class Second {
    record Status(String second) {}
  }
}
