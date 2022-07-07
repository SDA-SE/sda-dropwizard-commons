package org.sdase.commons.server.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import org.sdase.commons.server.jackson.errors.*;
import org.sdase.commons.server.jackson.filter.JacksonFieldFilterModule;
import org.sdase.commons.server.jackson.hal.HalLinkProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures the {@link ObjectMapper} to support HAL structures using {@link
 * io.openapitools.jackson.dataformat.hal.annotation.Resource}, {@link
 * io.openapitools.jackson.dataformat.hal.annotation.Link} and {@link
 * io.openapitools.jackson.dataformat.hal.annotation.EmbeddedResource} and field filtering on client
 * request for resources annotated by {@link EnableFieldFilter}.
 *
 * <p>The module registers itself when created in the {@link
 * io.dropwizard.Application#run(Configuration, Environment) run method} of the application:
 *
 * <pre>{@code
 * public void run(SolutionServiceConfiguration configuration, Environment environment) {
 *   // ...
 *   JacksonConfigurationBundle.builder(environment).build();
 *   // ...
 * }
 *
 * }</pre>
 *
 * <p>If Jacksons yaml provider is available in the classpath it will be registered as well so that
 * the application is able to respond to "Accept application/yaml" requests. The JacksonYAMLProvider
 * is available with {@code
 * com.fasterxml.jackson.jaxrs:jackson-jaxrs-yaml-provider:[jacksonVersion]}. The class will be
 * loaded dynamically to avoid a forced runtime dependency.
 */
public class JacksonConfigurationBundle implements ConfiguredBundle<Configuration> {

  private static final Logger LOG = LoggerFactory.getLogger(JacksonConfigurationBundle.class);

  private final boolean disableFieldFilter;

  private ObjectMapperConfigurationUtil.Builder objectMapperBuilder;

  public static Builder builder() {
    return new Builder();
  }

  private JacksonConfigurationBundle(
      ObjectMapperConfigurationUtil.Builder objectMapperBuilder, boolean disableFieldFilter) {
    this.objectMapperBuilder = objectMapperBuilder;
    this.disableFieldFilter = disableFieldFilter;
  }

  /**
   * Initializes the {@link ObjectMapper} as in the default {@link Bootstrap} but does not add the
   * {@code FuzzyEnumModule} as it breaks enum features of Jackson.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // Overwrite with custom defaults
    bootstrap.setObjectMapper(objectMapperBuilder.build());

    // Configure the Hibernate Validator to ask Jackson for property names
    bootstrap.setValidatorFactory(
        Validators.newConfiguration()
            .propertyNodeNameProvider(new JacksonPropertyNodeNameProvider())
            .buildValidatorFactory());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    disableDefaultErrorMappers(configuration);

    ObjectMapper objectMapper = environment.getObjectMapper();
    registerYamlProviderIfAvailable(environment);

    if (!disableFieldFilter) {
      JacksonFieldFilterModule jacksonFieldFilterModule = new JacksonFieldFilterModule();
      objectMapper.registerModule(jacksonFieldFilterModule);
      environment.jersey().register(jacksonFieldFilterModule);
    }

    // register singleton HalLinkProvider
    environment.jersey().register(HalLinkProvider.getInstance());

    // register Exception Mapper
    environment.jersey().register(ApiExceptionMapper.class);
    environment.jersey().register(JerseyValidationExceptionMapper.class);
    environment.jersey().register(ValidationExceptionMapper.class);
    environment.jersey().register(EarlyEofExceptionMapper.class);
    environment.jersey().register(JsonProcessingExceptionMapper.class);
    environment.jersey().register(WebApplicationExceptionMapper.class);
    environment.jersey().register(RuntimeExceptionMapper.class);
    environment.jersey().register(InvalidTypeIdExceptionMapper.class);
  }

  /**
   * Disables the default exception mappers. Usually they are overwritten by the custom mappers
   * registered in this bundle. But in some situations when CDI is used, the default mappers would
   * still be preferred by Dropwizard.
   *
   * @param configuration the configuration that is modified
   */
  private void disableDefaultErrorMappers(Configuration configuration) {
    if (configuration.getServerFactory() instanceof AbstractServerFactory) {
      ((AbstractServerFactory) configuration.getServerFactory())
          .setRegisterDefaultExceptionMappers(Boolean.FALSE);
    } else {
      throw new IllegalStateException(
          "Could not disable default exception mappers. "
              + "Expecting an AbstractServerFactory but got "
              + configuration.getServerFactory().getClass());
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

    private boolean disableFieldFilter = false;

    private ObjectMapperConfigurationUtil.Builder objectMapperBuilder =
        ObjectMapperConfigurationUtil.configureMapper();

    private Builder() {}

    /**
     * Skips registration of the HAL module. This may be used when links and embedded resources are
     * not required or are achieved with other tooling.
     *
     * @return the builder
     */
    public Builder withoutHalSupport() {
      objectMapperBuilder.withoutHalSupport();
      return this;
    }

    /**
     * Disables the field filter entirely. The field filter may be used by clients to request only a
     * subset of the properties of a resource and has to be activated with {@link EnableFieldFilter}
     * for each resource.
     *
     * @return the builder
     */
    public Builder withoutFieldFilter() {
      this.disableFieldFilter = true;
      return this;
    }

    /**
     * Allows customization of the used {@link ObjectMapper}. More customizers may be added by
     * calling this method multiple times.
     *
     * @param customizer receives the used {@link ObjectMapper} for customization, e.g. to enable or
     *     disable specific features or configure formatting.
     * @return the builder
     */
    public Builder withCustomization(Consumer<ObjectMapper> customizer) {
      objectMapperBuilder.withCustomization(customizer);
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
      objectMapperBuilder.alwaysWriteZonedDateTimeWithMillis();
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
      objectMapperBuilder.alwaysWriteZonedDateTimeWithoutMillis();
      return this;
    }

    public JacksonConfigurationBundle build() {
      return new JacksonConfigurationBundle(objectMapperBuilder, disableFieldFilter);
    }
  }
}
