package org.sdase.commons.server.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.CaffeineModule;
import io.dropwizard.jackson.GuavaExtrasModule;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.openapitools.jackson.dataformat.hal.JacksonHALModule;
import java.text.DateFormat;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.TimeZone;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the {@link ObjectMapper} to support HAL structures using {@link
 * io.openapitools.jackson.dataformat.hal.annotation.Resource}, {@link
 * io.openapitools.jackson.dataformat.hal.annotation.Link} and {@link
 * io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource}.
 */
public class ObjectMapperConfigurationUtil {
  private static final Logger LOG = LoggerFactory.getLogger(ObjectMapperConfigurationUtil.class);

  private ObjectMapperConfigurationUtil() {
    // Utility class
  }

  public static Builder configureMapper() {
    return new Builder();
  }

  public static class Builder {

    private Consumer<ObjectMapper> customizer = om -> {};

    private boolean disableHalSupport = false;

    private Iso8601Serializer defaultSerializer;

    private Builder() {}

    /**
     * Skips registration of the HAL module. This may be used when links and embedded resources are
     * not required or are achieved with other tooling.
     *
     * @return the builder
     */
    public Builder withoutHalSupport() {
      this.disableHalSupport = true;
      return this;
    }

    /**
     * Allows customization of the {@link ObjectMapper}. More customizers may be added by calling
     * this method multiple times.
     *
     * @param customizer receives the {@link ObjectMapper} for customization, e.g. to enable or
     *     disable specific features or configure formatting.
     * @return the builder
     */
    public Builder withCustomization(Consumer<ObjectMapper> customizer) {
      this.customizer = this.customizer.andThen(customizer);
      return this;
    }

    /**
     * Registers a default serializer for {@link ZonedDateTime} that renders 3 digits of
     * milliseconds. The same serializer may be configured per field as documented in {@link
     * Iso8601Serializer.WithMillis}.
     *
     * <p>This setting overwrites the default behaviour of Jackson which omits milliseconds if they
     * are zero or adds nanoseconds if they are set.
     *
     * @return the builder
     */
    public Builder alwaysWriteZonedDateTimeWithMillis() {
      this.defaultSerializer = new Iso8601Serializer.WithMillis();
      return this;
    }

    /**
     * Registers a default serializer for {@link ZonedDateTime} that renders no milliseconds. The
     * same serializer may be configured per field as documented in {@link Iso8601Serializer}.
     *
     * <p>This setting overwrites the default behaviour of Jackson which omits milliseconds if they
     * are zero or adds nanoseconds if they are set.
     *
     * @return the builder
     */
    public Builder alwaysWriteZonedDateTimeWithoutMillis() {
      this.defaultSerializer = new Iso8601Serializer();
      return this;
    }

    public ObjectMapper build() {
      // Overwrite the full featured default from Jackson.newObjectMapper() as some
      // registered modules break Jackson functionality. Add all features from
      // io.dropwizard.jackson.Jackson.newObjectMapper() but the FuzzyEnumModule
      // and modules registered in Jackson.newMinimalObjectMapper()
      ObjectMapper objectMapper =
          Jackson.newMinimalObjectMapper()
              // .registerModule(new GuavaModule()) in newMinimalObjectMapper
              .registerModule(new GuavaExtrasModule())
              .registerModule(new JodaModule())
              .registerModule(new AfterburnerModule())
              // .registerModule(new FuzzyEnumModule()) breaks READ_UNKNOWN_ENUM_VALUES_AS_NULL
              .registerModule(new ParameterNamesModule())
              .registerModule(new Jdk8Module())
              .registerModule(new JavaTimeModule())
              .registerModule(new CaffeineModule())
              .setPropertyNamingStrategy(new AnnotationSensitivePropertyNamingStrategy())
          // .setSubtypeResolver(new DiscoverableSubtypeResolver()) in  newMinimalObjectMapper
          ;

      configureObjectMapper(objectMapper);

      if (this.defaultSerializer != null) {
        SimpleModule module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class, this.defaultSerializer);
        objectMapper.registerModule(module);
      }

      customizer.accept(objectMapper);

      if (!disableHalSupport) {
        objectMapper.registerModule(new JacksonHALModule());
      }

      return objectMapper;
    }

    /**
     * Initializes the {@link ObjectMapper} as in the default {@link Bootstrap} but does not add the
     * {@code FuzzyEnumModule} as it breaks enum features of Jackson.
     */
    private void configureObjectMapper(ObjectMapper objectMapper) {
      // Platform configurations that exceed the Dropwizard defaults
      objectMapper
          // serialization
          .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
          // deserialization
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
          .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
          .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
          .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
          .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
          // time zone handling
          .setTimeZone(TimeZone.getTimeZone(ZoneOffset.UTC));

      configureRenderingOfColonInTimeZone(objectMapper);
    }

    private void configureRenderingOfColonInTimeZone(ObjectMapper objectMapper) {
      // render colon in time zone
      DateFormat original = objectMapper.getDateFormat();
      if (original instanceof StdDateFormat) {
        StdDateFormat stdDateFormat = (StdDateFormat) original;
        StdDateFormat withColonInTimeZone = stdDateFormat.withColonInTimeZone(true);
        objectMapper.setDateFormat(withColonInTimeZone);
      } else {
        LOG.warn(
            "Could not customize date format. Expecting StdDateFormat, but found {}",
            original.getClass());
      }
    }
  }
}
