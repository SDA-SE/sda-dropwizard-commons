package com.sdase.commons.server.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sdase.commons.server.jackson.filter.JacksonFieldFilterModule;
import io.dropwizard.Bundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.openapitools.jackson.dataformat.hal.JacksonHALModule;
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

   private boolean disableHalSupport;

   private boolean disableFieldFilter;

   private Consumer<ObjectMapper> objectMapperCustomizer;

   public static Builder builder() {
      return new Builder();
   }

   private JacksonConfigurationBundle(boolean disableHalSupport, boolean disableFieldFilter, Consumer<ObjectMapper> objectMapperCustomizer) {
      this.disableHalSupport = disableHalSupport;
      this.disableFieldFilter = disableFieldFilter;
      this.objectMapperCustomizer = objectMapperCustomizer;
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {
      // no initialization needed
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
   }

   private void configureObjectMapper(ObjectMapper objectMapper) {
      objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
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
       */
      public Builder withoutHalSupport() {
         this.disableHalSupport = true;
         return this;
      }

      /**
       * Disables the field filter entirely. The field filter may be used by clients to request only a subset of the
       * properties of a resource and has to be activated with {@link EnableFieldFilter} for each resource.
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
