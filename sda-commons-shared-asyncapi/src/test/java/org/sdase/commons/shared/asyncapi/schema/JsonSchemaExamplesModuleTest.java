package org.sdase.commons.shared.asyncapi.schema;

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_7;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class JsonSchemaExamplesModuleTest {

  @Test()
  public void shouldGenerateExamplesFromAnnotations() {
    SchemaGeneratorConfig config =
        new SchemaGeneratorConfigBuilder(DRAFT_7, PLAIN_JSON)
            .with(new JsonSchemaExamplesModule())
            .build();
    SchemaGenerator generator = new SchemaGenerator(config);
    ObjectNode schemaNode = generator.generateSchema(ExampleModel.class);
    Map<String, Object> schema =
        config
            .getObjectMapper()
            .convertValue(schemaNode, new TypeReference<Map<String, Object>>() {});

    assertThat(schema).extracting("properties.name.examples").isEqualTo(asList("Chantal", "Kevin"));
    assertThat(schema).extracting("properties.age.examples").isEqualTo(asList(25, 34));
    assertThat(schema).extracting("properties.income.examples").isEqualTo(singletonList(45.3));
    assertThat(schema)
        .extracting("properties.interests.examples")
        .isEqualTo(singletonList(asList("soccer", "makeup")));
    assertThat(schema)
        .extracting("properties.interests.items.examples")
        .as("No not include the examples for each list item")
        .isNull();
  }

  public static class ExampleModel {

    @JsonSchemaExamples({"Chantal", "Kevin"})
    private String name;

    @JsonSchemaExamples({"25", "34"})
    private int age;

    @JsonSchemaExamples({"45.3"})
    private BigDecimal income;

    @JsonSchemaExamples({"[\"soccer\", \"makeup\"]"})
    private List<String> interests;
  }
}
