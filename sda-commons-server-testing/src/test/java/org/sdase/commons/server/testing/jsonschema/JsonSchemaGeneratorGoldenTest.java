package org.sdase.commons.server.testing.jsonschema;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;

class JsonSchemaGeneratorGoldenTest {

  private static final Path CUSTOMER_CREATED_SCHEMA_GOLDEN_FILE =
      Path.of("src/test/resources/customer-created-schema.json");
  private static final Path SCHEMA_CATALOG_GOLDEN_FILE =
      Path.of("src/test/resources/schema-catalog.json");

  @Test
  void generatesDocumentedCustomerCreatedSchema() throws IOException {
    var schema = JsonSchemaGenerator.defaultGenerator().generate(CustomerCreated.class);
    String actual = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema);

    GoldenFileAssertions.assertThat(CUSTOMER_CREATED_SCHEMA_GOLDEN_FILE)
        .hasYamlContentAndUpdateGolden(actual);
  }

  @Test
  void generatesSchemaCatalog() throws IOException {
    Type customerCreatedList = new TypeReference<List<CustomerCreated>>() {}.getType();
    var schema =
        JsonSchemaGenerator.defaultGenerator()
            .generate(List.of(CustomerCreated.class, CustomerUpdated.class, customerCreatedList));
    String actual = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(schema);

    GoldenFileAssertions.assertThat(SCHEMA_CATALOG_GOLDEN_FILE)
        .hasYamlContentAndUpdateGolden(actual);
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
