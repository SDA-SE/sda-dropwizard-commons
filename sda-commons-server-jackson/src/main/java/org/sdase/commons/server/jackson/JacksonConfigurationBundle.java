package org.sdase.commons.server.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.GuavaExtrasModule;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.JacksonHALModule;
import org.sdase.commons.server.jackson.errors.ApiExceptionMapper;
import org.sdase.commons.server.jackson.errors.EarlyEofExceptionMapper;
import org.sdase.commons.server.jackson.errors.JerseyValidationExceptionMapper;
import org.sdase.commons.server.jackson.errors.JsonParseExceptionMapper;
import org.sdase.commons.server.jackson.errors.ValidationExceptionMapper;
import org.sdase.commons.server.jackson.errors.WebApplicationExceptionMapper;
import org.sdase.commons.server.jackson.filter.JacksonFieldFilterModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * <p>
 *    Configures the {@link ObjectMapper} to support HAL structures using
 *    {@link io.openapitools.jackson.dataformat.hal.annotation.Resource},
 *    {@link io.openapitools.jackson.dataformat.hal.annotation.Link} and
 *    {@link io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource} and field filtering on client request
 *    for resources annotated by {@link EnableFieldFilter}.
 * </p>
 * <p>
 *    The module registers itself when created in the
 *    {@link io.dropwizard.Application#run(Configuration, Environment) run method} of the application:
 * </p>
 * <pre>
 *    {@code
 *       public void run(SolutionServiceConfiguration configuration, Environment environment) {
 *         // ...
 *         JacksonConfigurationBundle.builder(environment).build();
 *         // ...
 *       }
 *    }
 * </pre>
 * <p>
 *    If Jacksons yaml provider is available in the classpath it will be registered as well so that the application is
 *    able to respond to "Accept application/yaml" requests. The JacksonYAMLProvider is available with
 *    {@code com.fasterxml.jackson.jaxrs:jackson-jaxrs-yaml-provider:[jacksonVersion]}. The class will be loaded
 *    dynamically to avoid a forced runtime dependency.
 * </p>
 */
public class JacksonConfigurationBundle implements Bundle {

   private static final Logger LOG = LoggerFactory.getLogger(JacksonConfigurationBundle.class);

   private final boolean disableHalSupport;

   private final boolean disableFieldFilter;

   private final Consumer<ObjectMapper> objectMapperCustomizer;

   public static Builder builder() {
      return new Builder();
   }

   private JacksonConfigurationBundle(boolean disableHalSupport, boolean disableFieldFilter, Consumer<ObjectMapper> objectMapperCustomizer) {
      this.disableHalSupport = disableHalSupport;
      this.disableFieldFilter = disableFieldFilter;
      this.objectMapperCustomizer = objectMapperCustomizer;
   }

   /**
    * Initializes the {@link ObjectMapper} as in the default {@link Bootstrap} but does not add the
    * {@code FuzzyEnumModule} as it breaks enum features of Jackson.
    *
    * {@inheritDoc}
    */
   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // Overwrite the full featured default from Jackson.newObjectMapper() as some
      // registered modules break Jackson functionality. Add all features from
      // io.dropwizard.jackson.Jackson.newObjectMapper() but the FuzzyEnumModule
      // and modules registered in Jackson.newMinimalObjectMapper()
      ObjectMapper objectMapper = Jackson.newMinimalObjectMapper()
            // .registerModule(new GuavaModule()) in newMinimalObjectMapper
            .registerModule(new GuavaExtrasModule())
            .registerModule(new JodaModule())
            .registerModule(new AfterburnerModule())
            // .registerModule(new FuzzyEnumModule()) breaks READ_UNKNOWN_ENUM_VALUES_AS_NULL
            .registerModule(new ParameterNamesModule())
            .registerModule(new Jdk8Module())
            .registerModule(new JavaTimeModule())
            .setPropertyNamingStrategy(new AnnotationSensitivePropertyNamingStrategy())
            // .setSubtypeResolver(new DiscoverableSubtypeResolver()) in newMinimalObjectMapper
      ;

      // Overwrite with custom defaults
      bootstrap.setObjectMapper(objectMapper);

   }

   @Override
   public void run(Environment environment) {
      ObjectMapper objectMapper = environment.getObjectMapper();
      registerYamlProviderIfAvailable(environment);
      configureObjectMapper(objectMapper);

      if (!disableHalSupport) {
         objectMapper.registerModule(new JacksonHALModule());
      }
      if (!disableFieldFilter) {
         JacksonFieldFilterModule jacksonFieldFilterModule = new JacksonFieldFilterModule();
         environment.jersey().register(this);
         environment.jersey().register(jacksonFieldFilterModule);
         objectMapper.registerModule(jacksonFieldFilterModule);
      }

      // register Exception Mapper (seems to overwrite default exception mapper)
      environment.jersey().register(ApiExceptionMapper.class);
      environment.jersey().register(JerseyValidationExceptionMapper.class);
      environment.jersey().register(ValidationExceptionMapper.class);
      environment.jersey().register(EarlyEofExceptionMapper.class);
      environment.jersey().register(JsonParseExceptionMapper.class);
      environment.jersey().register(WebApplicationExceptionMapper.class);


   }

   private void configureObjectMapper(ObjectMapper objectMapper) {
      // Platform configurations that exceed the Dropwizard defaults
      objectMapper
            // serialization
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            // deserialization
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
            .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
      ;
      // Application specific configuration
      if (this.objectMapperCustomizer != null) {
         this.objectMapperCustomizer.accept(objectMapper);
      }
   }

   private void registerYamlProviderIfAvailable(Environment environment) {
      String className = "com.fasterxml.jackson.jaxrs.yaml.JacksonYAMLProvider";
      try {
         Class<?> jacksonYamlProvider = this.getClass().getClassLoader().loadClass(className);
         environment.jersey().register(jacksonYamlProvider);
      } catch (ClassNotFoundException e) {
         LOG.info("{} not found. Not registering provider to render Yaml responses.", className);
      }
   }

   public static class Builder {

      private boolean disableHalSupport = false;

      private boolean disableFieldFilter = false;

      private Consumer<ObjectMapper> customizer = om -> {};

      private Builder() {
      }

      /**
       * Skips registration of the HAL module. This may be used when links and embedded resources are not required or
       * are achieved with other tooling.
       *
       * @return the builder
       */
      public Builder withoutHalSupport() {
         this.disableHalSupport = true;
         return this;
      }

      /**
       * Disables the field filter entirely. The field filter may be used by clients to request only a subset of the
       * properties of a resource and has to be activated with {@link EnableFieldFilter} for each resource.
       *
       * @return the builder
       */
      public Builder withoutFieldFilter() {
         this.disableFieldFilter = true;
         return this;
      }

      /**
       * Allows customization of the used {@link ObjectMapper}. More customizers may be added by calling this method
       * multiple times.
       * @param customizer receives the used {@link ObjectMapper} for customization, e.g. to enable or disable specific
       *                   features or configure formatting.
       * @return the builder
       */
      public Builder withCustomization(Consumer<ObjectMapper> customizer) {
         this.customizer = this.customizer.andThen(customizer);
         return this;
      }

      public JacksonConfigurationBundle build() {
         return new JacksonConfigurationBundle(disableHalSupport, disableFieldFilter, customizer);
      }
   }
}
