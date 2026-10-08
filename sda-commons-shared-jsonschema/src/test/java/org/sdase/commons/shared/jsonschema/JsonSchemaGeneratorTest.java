package org.sdase.commons.shared.jsonschema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
  void generatesStandaloneDraft7SchemaWithValidationAndExamples() {
    ObjectNode schema = generator.generate(ContractBundle.class);

    assertThat(schema.path("$schema").asText())
        .isEqualTo("http://json-schema.org/draft-07/schema#");
    assertThat(schema.at("/definitions/ContractBundle/properties/id/minLength").asInt())
        .isEqualTo(1);
    assertThat(schema.at("/definitions/ContractBundle/properties/id/pattern").asText())
        .isEqualTo("^.*\\S+.*$");
    assertThat(schema.at("/definitions/ContractBundle/properties/id/examples/0").asText())
        .isEqualTo("contract-42");
    assertInternalReferencesResolve(schema);
  }

  @Test
  void generatesCatalogWithObjectAndListRoots() {
    Type listOfContractBundles = new TypeReference<List<ContractBundle>>() {}.getType();

    ObjectNode catalog = generator.generate(List.of(ContractBundle.class, listOfContractBundles));

    assertThat(catalog.path("$schema").asText())
        .isEqualTo("http://json-schema.org/draft-07/schema#");
    assertThat(catalog.at("/oneOf/0/$ref").asText()).isEqualTo("#/definitions/ContractBundle");
    assertThat(catalog.at("/definitions/ContractBundle/type").asText()).isEqualTo("object");
    assertThat(catalog.at("/oneOf/1/type").asText()).isEqualTo("array");
    assertThat(catalog.at("/oneOf/1/items/$ref").asText())
        .isEqualTo("#/definitions/ContractBundle");
    assertInternalReferencesResolve(catalog);
  }

  @Test
  void keepsSameSimpleNameModelsAsSeparateDefinitions() {
    ObjectNode schema = generator.generate(DuplicateNames.class);

    String firstReference = schema.at("/definitions/DuplicateNames/properties/first/$ref").asText();
    String secondReference =
        schema.at("/definitions/DuplicateNames/properties/second/$ref").asText();

    assertThat(firstReference).startsWith("#/definitions/").isNotEqualTo(secondReference);
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

  static class ContractBundle {
    @NotBlank
    @Schema(example = "contract-42")
    private String id;

    public String getId() {
      return id;
    }
  }

  static class DuplicateNames {
    private First.Status first;
    private Second.Status second;

    public First.Status getFirst() {
      return first;
    }

    public Second.Status getSecond() {
      return second;
    }
  }

  static class First {
    static class Status {
      private String first;

      public String getFirst() {
        return first;
      }
    }
  }

  static class Second {
    static class Status {
      private String second;

      public String getSecond() {
        return second;
      }
    }
  }
}
