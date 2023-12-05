/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.test.data.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.*;
import java.util.Date;

/** see {@code AbstractJsonSchemaBuilderTest#shouldSetSpecificFieldsOfDefinition()} */
@SuppressWarnings("unused")
public interface MinimalTestModels {

  interface Required {

    class JakartaNotBlank {
      @NotBlank private String notBlankProperty;

      public String getNotBlankProperty() {
        return notBlankProperty;
      }

      public JakartaNotBlank setNotBlankProperty(String notBlankProperty) {
        this.notBlankProperty = notBlankProperty;
        return this;
      }
    }

    class JakartaNotNull {
      @NotNull private String requiredProperty;

      public String getRequiredProperty() {
        return requiredProperty;
      }

      public JakartaNotNull setRequiredProperty(String requiredProperty) {
        this.requiredProperty = requiredProperty;
        return this;
      }
    }

    class SwaggerRequiredMode {
      @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
      private String requiredProperty;

      public String getRequiredProperty() {
        return requiredProperty;
      }

      public SwaggerRequiredMode setRequiredProperty(String requiredProperty) {
        this.requiredProperty = requiredProperty;
        return this;
      }
    }

    class SwaggerRequired {
      @SuppressWarnings("deprecation")
      @Schema(required = true)
      private String requiredProperty;

      public String getRequiredProperty() {
        return requiredProperty;
      }

      public SwaggerRequired setRequiredProperty(String requiredProperty) {
        this.requiredProperty = requiredProperty;
        return this;
      }
    }

    class JsonPropertyRequired {
      @JsonProperty(required = true)
      private String requiredProperty;

      public String getRequiredProperty() {
        return requiredProperty;
      }

      public JsonPropertyRequired setRequiredProperty(String requiredProperty) {
        this.requiredProperty = requiredProperty;
        return this;
      }
    }
  }

  interface Temporal {

    class TemporalDate {
      private Date dateTime;

      public Date getDateTime() {
        return dateTime;
      }

      public TemporalDate setDateTime(Date dateTime) {
        this.dateTime = dateTime;
        return this;
      }
    }

    class TemporalZonedDateTime {
      private ZonedDateTime dateTime;

      public ZonedDateTime getDateTime() {
        return dateTime;
      }

      public TemporalZonedDateTime setDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        return this;
      }
    }

    class TemporalOffsetDateTime {
      private ZonedDateTime dateTime;

      public ZonedDateTime getDateTime() {
        return dateTime;
      }

      public TemporalOffsetDateTime setDateTime(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        return this;
      }
    }

    class TemporalInstant {
      private Instant dateTime;

      public Instant getDateTime() {
        return dateTime;
      }

      public TemporalInstant setDateTime(Instant dateTime) {
        this.dateTime = dateTime;
        return this;
      }
    }

    class TemporalLocalDate {
      private LocalDate date;

      public LocalDate getDate() {
        return date;
      }

      public TemporalLocalDate setDate(LocalDate date) {
        this.date = date;
        return this;
      }
    }

    class TemporalDuration {
      private Duration duration;

      public Duration getDuration() {
        return duration;
      }

      public TemporalDuration setDuration(Duration duration) {
        this.duration = duration;
        return this;
      }
    }

    class TemporalPeriod {
      private Period duration;

      public Period getDuration() {
        return duration;
      }

      public TemporalPeriod setDuration(Period duration) {
        this.duration = duration;
        return this;
      }
    }
  }

  interface Description {

    class DescriptionSwaggerSchema {

      @Schema(description = "A property")
      private String described;

      public String getDescribed() {
        return described;
      }

      public DescriptionSwaggerSchema setDescribed(String described) {
        this.described = described;
        return this;
      }
    }

    class DescriptionJsonProperty {
      @JsonPropertyDescription("A property")
      private String described;

      public String getDescribed() {
        return described;
      }

      public DescriptionJsonProperty setDescribed(String described) {
        this.described = described;
        return this;
      }
    }
  }

  class UriProperties {
    private URI notRequiredUri;
    @NotBlank private URI notBlankUri;

    public URI getNotRequiredUri() {
      return notRequiredUri;
    }

    public UriProperties setNotRequiredUri(URI notRequiredUri) {
      this.notRequiredUri = notRequiredUri;
      return this;
    }

    public URI getNotBlankUri() {
      return notBlankUri;
    }

    public UriProperties setNotBlankUri(URI notBlankUri) {
      this.notBlankUri = notBlankUri;
      return this;
    }
  }

  interface Enums {
    enum EnumPlain {
      ONE,
      TWO
    }

    enum EnumJackson {
      @JsonProperty("1")
      ONE,
      @JsonProperty("2")
      TWO
    }
  }
}
