package org.sdase.commons.server.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.consumer.ConsumerTokenBundle.ConsumerTokenConfigProvider;
import org.sdase.commons.server.consumer.ConsumerTokenConfig;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.server.cors.CorsConfigProvider;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.dropwizard.bundles.DefaultLoggingConfigurationBundle;
import org.sdase.commons.server.healthcheck.InternalHealthCheckEndpointBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.jaeger.JaegerBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.OpaBundle.OpaBuilder;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.server.openapi.OpenApiBundle;
import org.sdase.commons.server.opentracing.OpenTracingBundle;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.AuthConfigProviderBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.ConsumerTokenConfigBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.ConsumerTokenRequiredConfigInitialBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.CorsConfigProviderBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.OpenApiFinalBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.OpenApiInitialBuilder;
import org.sdase.commons.server.starter.builder.InitialPlatformBundleBuilder;
import org.sdase.commons.server.starter.builder.PlatformBundleBuilder;
import org.sdase.commons.server.trace.TraceTokenBundle;

/**
 * A {@link ConfiguredBundle} that configures the application with the basics required for a SDA
 * platform compatible microservice.
 */
public class SdaPlatformBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private SecurityBundle.Builder securityBundleBuilder;
  private JacksonConfigurationBundle.Builder jacksonConfigurationBundleBuilder;
  private AuthBundle.AuthBuilder<C> authBundleBuilder;
  private OpaBuilder<C> opaBundleBuilder;
  private CorsBundle.FinalBuilder<C> corsBundleBuilder;
  private ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder;
  private OpenApiBundle.FinalBuilder openApiBundleBuilder;

  private SdaPlatformBundle(
      SecurityBundle.Builder securityBundleBuilder,
      JacksonConfigurationBundle.Builder jacksonConfigurationBundleBuilder,
      AuthBundle.AuthBuilder<C> authBundleBuilder,
      OpaBundle.OpaBuilder<C> opaBundleBuilder,
      CorsBundle.FinalBuilder<C> corsBundleBuilder,
      ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder,
      OpenApiBundle.FinalBuilder openApiBundleBuilder) {
    this.securityBundleBuilder = securityBundleBuilder;
    this.jacksonConfigurationBundleBuilder = jacksonConfigurationBundleBuilder;
    this.authBundleBuilder = authBundleBuilder;
    this.opaBundleBuilder = opaBundleBuilder;
    this.corsBundleBuilder = corsBundleBuilder;
    this.consumerTokenBundleBuilder = consumerTokenBundleBuilder;
    this.openApiBundleBuilder = openApiBundleBuilder;
  }

  public static InitialPlatformBundleBuilder builder() {
    return new InitialBuilder<>();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {

    // add normal bundles
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(DefaultLoggingConfigurationBundle.builder().build());
    bootstrap.addBundle(InternalHealthCheckEndpointBundle.builder().build());
    bootstrap.addBundle(JaegerBundle.builder().build());
    bootstrap.addBundle(OpenTracingBundle.builder().build());
    bootstrap.addBundle(PrometheusBundle.builder().build());
    bootstrap.addBundle(TraceTokenBundle.builder().build());

    // add configured bundles
    List<ConfiguredBundle<? super C>> configuredBundles = new ArrayList<>();
    configuredBundles.add(jacksonConfigurationBundleBuilder.build());
    configuredBundles.add(securityBundleBuilder.build());
    configuredBundles.add(openApiBundleBuilder.build());
    if (authBundleBuilder != null) {
      configuredBundles.add(authBundleBuilder.build());
    }
    if (opaBundleBuilder != null) {
      configuredBundles.add(opaBundleBuilder.build());
    }
    if (corsBundleBuilder != null) {
      configuredBundles.add(corsBundleBuilder.build());
    }
    if (consumerTokenBundleBuilder != null) {
      configuredBundles.add(consumerTokenBundleBuilder.build());
    }
    configuredBundles.stream().map(b -> (ConfiguredBundle) b).forEach(bootstrap::addBundle);
  }

  @Override
  public void run(C configuration, Environment environment) {
    // not needed for the platform bundle, created bundles are added in initialize
  }

  public static class InitialBuilder<C extends Configuration>
      implements InitialPlatformBundleBuilder,
          AuthConfigProviderBuilder<C>,
          CorsConfigProviderBuilder<C>,
          ConsumerTokenConfigBuilder<C>,
          ConsumerTokenRequiredConfigInitialBuilder<C>,
          OpenApiInitialBuilder<C>,
          OpenApiFinalBuilder<C>,
          PlatformBundleBuilder<C> {

    private AuthBundle.AuthBuilder<C> authBundleBuilder;
    private OpaBundle.OpaBuilder<C> opaBundleBuilder;
    private ConsumerTokenConfig consumerTokenConfig;
    private ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder;
    private SecurityBundle.Builder securityBundleBuilder = SecurityBundle.builder();
    private JacksonConfigurationBundle.Builder jacksonBundleBuilder =
        JacksonConfigurationBundle.builder();
    private CorsBundle.FinalBuilder<C> corsBundleBuilder;
    private OpenApiBundle.FinalBuilder openApiBundleBuilder;

    private InitialBuilder() {}

    // Final step

    @Override
    public SdaPlatformBundle<C> build() {
      return new SdaPlatformBundle<>(
          securityBundleBuilder,
          jacksonBundleBuilder,
          authBundleBuilder,
          opaBundleBuilder,
          corsBundleBuilder,
          consumerTokenBundleBuilder,
          openApiBundleBuilder);
    }

    // InitialBuilder

    @Override
    public ConsumerTokenConfigBuilder<SdaPlatformConfiguration> usingSdaPlatformConfiguration() {
      return usingCustomConfig(SdaPlatformConfiguration.class)
          .withAuthConfigProvider(SdaPlatformConfiguration::getAuth)
          .withCorsConfigProvider(SdaPlatformConfiguration::getCors);
    }

    @Override
    public <T extends Configuration> AuthConfigProviderBuilder<T> usingCustomConfig(
        Class<T> configurationClass) {
      return new InitialBuilder<>();
    }

    // CustomConfigurationProviders and follow up configuration providers

    @Override
    public CorsConfigProviderBuilder<C> withoutAuthentication() {
      return this;
    }

    @Override
    public CorsConfigProviderBuilder<C> withAuthConfigProvider(
        AuthConfigProvider<C> authConfigProvider) {
      this.authBundleBuilder =
          AuthBundle.builder()
              .withAuthConfigProvider(authConfigProvider)
              .withAnnotatedAuthorization();
      return this;
    }

    @Override
    public CorsConfigProviderBuilder<C> withOpaAuthorization(
        AuthConfigProvider<C> authConfigProvider, OpaConfigProvider<C> opaConfigProvider) {
      this.authBundleBuilder =
          AuthBundle.builder()
              .withAuthConfigProvider(authConfigProvider)
              .withExternalAuthorization();
      this.opaBundleBuilder = OpaBundle.builder().withOpaConfigProvider(opaConfigProvider);
      return this;
    }

    @Override
    public ConsumerTokenConfigBuilder<C> withoutCorsSupport() {
      return this;
    }

    @Override
    public ConsumerTokenConfigBuilder<C> withCorsConfigProvider(
        CorsConfigProvider<C> corsConfigProvider) {
      this.corsBundleBuilder = CorsBundle.builder().withCorsConfigProvider(corsConfigProvider);
      return this;
    }

    @Override
    public OpenApiInitialBuilder<C> withoutConsumerTokenSupport() {
      return this;
    }

    @Override
    public OpenApiInitialBuilder<C> withOptionalConsumerToken() {
      consumerTokenConfig = new ConsumerTokenConfig();
      consumerTokenConfig.setOptional(true);
      this.consumerTokenBundleBuilder =
          ConsumerTokenBundle.builder().withConfigProvider(c -> consumerTokenConfig);
      return this;
    }

    @Override
    public ConsumerTokenRequiredConfigInitialBuilder<C> withRequiredConsumerToken() {
      consumerTokenConfig = new ConsumerTokenConfig();
      consumerTokenConfig.setOptional(false);
      this.consumerTokenBundleBuilder =
          ConsumerTokenBundle.builder().withConfigProvider(c -> consumerTokenConfig);
      return this;
    }

    @Override
    public OpenApiInitialBuilder<C> withConsumerTokenConfigProvider(
        ConsumerTokenConfigProvider<C> consumerTokenConfigProvider) {
      this.consumerTokenBundleBuilder =
          ConsumerTokenBundle.builder().withConfigProvider(consumerTokenConfigProvider);
      return this;
    }

    @Override
    public OpenApiInitialBuilder<C> withExcludePatternsForRequiredConsumerToken(String... regex) {
      if (this.consumerTokenConfig == null) {
        throw new IllegalStateException(
            "ConsumerToken support can't be configured because it is disabled.");
      }
      this.consumerTokenConfig.getExcludePatterns().addAll(Arrays.asList(regex));
      return this;
    }

    // PlatformBundleBuilder contains programmatic configuration

    @Override
    public PlatformBundleBuilder<C> disableBufferLimitValidationSecurityFeature() {
      this.securityBundleBuilder = SecurityBundle.builder().disableBufferLimitValidation();
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withCorsAllowedMethods(String... httpMethods) {
      validateConfigureCors();
      this.corsBundleBuilder.withAllowedMethods(httpMethods);
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withCorsAdditionalAllowedHeaders(
        String... additionalAllowedHeaders) {
      validateConfigureCors();
      this.corsBundleBuilder.withAdditionalAllowedHeaders(additionalAllowedHeaders);
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withCorsAdditionalExposedHeaders(
        String... additionalExposedHeaders) {
      validateConfigureCors();
      this.corsBundleBuilder.withAdditionalExposedHeaders(additionalExposedHeaders);
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withoutHalSupport() {
      this.jacksonBundleBuilder.withoutHalSupport();
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withoutFieldFilter() {
      this.jacksonBundleBuilder.withoutFieldFilter();
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> withObjectMapperCustomization(
        Consumer<ObjectMapper> customizer) {
      this.jacksonBundleBuilder.withCustomization(customizer);
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> alwaysWriteZonedDateTimeWithMillisInJson() {
      this.jacksonBundleBuilder.alwaysWriteZonedDateTimeWithMillis();
      return this;
    }

    @Override
    public PlatformBundleBuilder<C> alwaysWriteZonedDateTimeWithoutMillisInJson() {
      this.jacksonBundleBuilder.alwaysWriteZonedDateTimeWithoutMillis();
      return this;
    }

    @Override
    public OpenApiFinalBuilder<C> withExistingOpenAPI(String openApiJsonOrYaml) {
      this.openApiBundleBuilder = getOpenApiBuilder().withExistingOpenAPI(openApiJsonOrYaml);
      return this;
    }

    @Override
    public OpenApiFinalBuilder<C> withExistingOpenAPIFromClasspathResource(String path) {
      this.openApiBundleBuilder =
          getOpenApiBuilder().withExistingOpenAPIFromClasspathResource(path);
      return this;
    }

    @Override
    public OpenApiFinalBuilder<C> addOpenApiResourcePackage(String resourcePackage) {
      this.openApiBundleBuilder = getOpenApiBuilder().addResourcePackage(resourcePackage);
      return this;
    }

    @Override
    public OpenApiFinalBuilder<C> addOpenApiResourcePackageClass(Class<?> swaggerResourcePackage) {
      this.openApiBundleBuilder =
          getOpenApiBuilder().addResourcePackageClass(swaggerResourcePackage);
      return this;
    }

    @Override
    public OpenApiFinalBuilder<C> disableSwaggerEmbedParameter() {
      this.openApiBundleBuilder.disableEmbedParameter();
      return this;
    }

    // helper

    private OpenApiBundle.InitialBuilder getOpenApiBuilder() {
      return openApiBundleBuilder != null ? openApiBundleBuilder : OpenApiBundle.builder();
    }

    private void validateConfigureCors() {
      if (this.corsBundleBuilder == null) {
        throw new IllegalStateException(
            "Attempt to configure CORS details, but CORS is not active.");
      }
    }
  }
}
