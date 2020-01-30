package org.sdase.commons.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.jackson.ObjectMapperConfigurationUtil.configureMapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kjetland.jackson.jsonSchema.JsonSchemaConfig;
import com.kjetland.jackson.jsonSchema.JsonSchemaGenerator;
import java.time.ZonedDateTime;
import org.junit.Test;

public class Iso8601SerializerTest {

  @Test
  public void shouldProduceDateTimeTypeHint() {

    ObjectMapper mapper = configureMapper().alwaysWriteZonedDateTimeWithMillis().build();

    JsonSchemaConfig config = JsonSchemaConfig.nullableJsonSchemaDraft4();
    JsonSchemaGenerator schemaGen = new JsonSchemaGenerator(mapper, config);
    JsonNode jsonNode = schemaGen.generateJsonSchema(ZonedDateTime.class);

    assertThat(jsonNode.get("format").asText()).isEqualTo("date-time");
  }
}
