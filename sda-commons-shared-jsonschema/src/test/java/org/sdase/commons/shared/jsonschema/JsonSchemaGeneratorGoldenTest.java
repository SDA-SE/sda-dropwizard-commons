package org.sdase.commons.shared.jsonschema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;

class JsonSchemaGeneratorGoldenTest {

  private static final Path EXPECTED_SCHEMA = Path.of("src/test/resources/schema_expected.json");

  @Test
  void generatesSchemaCatalog() throws IOException {
    Type customerCreatedList = new TypeReference<List<CustomerCreated>>() {}.getType();
    var schema =
        JsonSchemaGenerator.defaultGenerator()
            .generate(List.of(CustomerCreated.class, CustomerUpdated.class, customerCreatedList));
    String actual = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema);

    GoldenFileAssertions.assertThat(EXPECTED_SCHEMA).hasYamlContentAndUpdateGolden(actual);
  }

  @Schema(title = "Customer created", description = "Customer creation event")
  static class CustomerCreated {
    @NotBlank
    @Schema(description = "Customer identifier", example = "customer-42")
    private String customerId;

    @NotNull private Instant occurredAt;

    public String getCustomerId() {
      return customerId;
    }

    public Instant getOccurredAt() {
      return occurredAt;
    }
  }

  @Schema(title = "Customer updated", description = "Customer update event")
  static class CustomerUpdated {
    @NotBlank private String customerId;

    @NotBlank private String changedField;

    public String getCustomerId() {
      return customerId;
    }

    public String getChangedField() {
      return changedField;
    }
  }
}
