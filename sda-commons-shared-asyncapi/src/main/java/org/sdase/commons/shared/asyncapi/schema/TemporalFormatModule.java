package org.sdase.commons.shared.asyncapi.schema;

import com.github.victools.jsonschema.generator.FieldScope;
import com.github.victools.jsonschema.generator.Module;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Period;
import java.time.ZonedDateTime;
import java.util.Date;

/** A module that adds the correct <code>format</code> to properties of time related types. */
public class TemporalFormatModule implements Module {

  @Override
  public void applyToConfigBuilder(SchemaGeneratorConfigBuilder builder) {
    // Configure the format based on our object mapper configuration (see
    // ObjectMapperConfigurationUtil and DateFormatObjectMapperTest)
    builder.forFields().withStringFormatResolver(this::stringFormatResolver);
  }

  private String stringFormatResolver(FieldScope target) {
    if (target.getDeclaredType().isInstanceOf(OffsetDateTime.class)
        || target.getDeclaredType().isInstanceOf(Instant.class)
        || target.getDeclaredType().isInstanceOf(ZonedDateTime.class)
        || target.getDeclaredType().isInstanceOf(Date.class)) {
      return "date-time";
    } else if (target.getDeclaredType().isInstanceOf(LocalDate.class)) {
      return "date";
    } else if (target.getDeclaredType().isInstanceOf(Duration.class)
        || target.getDeclaredType().isInstanceOf(Period.class)) {
      return "duration";
    }
    return null;
  }
}
