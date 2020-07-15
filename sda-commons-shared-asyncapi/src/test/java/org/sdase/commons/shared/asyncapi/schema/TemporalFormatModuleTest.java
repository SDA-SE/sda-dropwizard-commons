package org.sdase.commons.shared.asyncapi.schema;

import static com.github.victools.jsonschema.generator.OptionPreset.PLAIN_JSON;
import static com.github.victools.jsonschema.generator.SchemaVersion.DRAFT_7;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfig;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Map;
import org.junit.Test;

public class TemporalFormatModuleTest {

  @Test()
  public void shouldGenerateFormatFromType() {
    SchemaGeneratorConfig config =
        new SchemaGeneratorConfigBuilder(DRAFT_7, PLAIN_JSON)
            .with(new TemporalFormatModule())
            .build();
    SchemaGenerator generator = new SchemaGenerator(config);
    ObjectNode schemaNode = generator.generateSchema(ExampleModel.class);
    Map<String, Object> schema =
        config
            .getObjectMapper()
            .convertValue(schemaNode, new TypeReference<Map<String, Object>>() {});

    assertThat(schema).extracting("properties.fieldOfOffsetDateTime.format").isEqualTo("date-time");
    assertThat(schema).extracting("properties.fieldOfInstant.format").isEqualTo("date-time");
    assertThat(schema).extracting("properties.fieldOfZonedDateTime.format").isEqualTo("date-time");
    assertThat(schema).extracting("properties.fieldOfDate.format").isEqualTo("date-time");

    assertThat(schema).extracting("properties.fieldOfLocalDate.format").isEqualTo("date");

    assertThat(schema).extracting("properties.fieldOfDuration.format").isEqualTo("duration");
    assertThat(schema).extracting("properties.fieldOfPeriod.format").isEqualTo("duration");
  }

  public static class ExampleModel {

    private OffsetDateTime fieldOfOffsetDateTime;
    private Instant fieldOfInstant;
    private ZonedDateTime fieldOfZonedDateTime;
    private Date fieldOfDate;

    private LocalDate fieldOfLocalDate;

    private Duration fieldOfDuration;
    private Period fieldOfPeriod;
  }
}
