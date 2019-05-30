package org.sdase.commons.server.starter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.models.Contact;
import io.swagger.models.Info;
import io.swagger.models.License;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.consumer.ConsumerTokenBundle.ConsumerTokenConfigProvider;
import org.sdase.commons.server.consumer.ConsumerTokenConfig;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.server.cors.CorsConfigProvider;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.healthcheck.InternalHealthCheckEndpointBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.dropwizard.bundles.DefaultLoggingConfigurationBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.OpaBundle.OpaBuilder;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.AuthConfigProviderBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.ConsumerTokenConfigBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.ConsumerTokenRequiredConfigBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.CorsConfigProviderBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.SwaggerDetailsBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.SwaggerFinalBuilder;
import org.sdase.commons.server.starter.builder.CustomConfigurationProviders.SwaggerTitleBuilder;
import org.sdase.commons.server.starter.builder.InitialPlatformBundleBuilder;
import org.sdase.commons.server.starter.builder.PlatformBundleBuilder;
import org.sdase.commons.server.swagger.SwaggerBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * A {@link ConfiguredBundle} that configures the application with the basics required for a SDA platform compatible
 * microservice.
 */
public class SdaPlatformBundle<C extends Configuration> implements ConfiguredBundle<C> {

   private SecurityBundle.Builder securityBundleBuilder;
   private JacksonConfigurationBundle.Builder jacksonConfigurationBundleBuilder;
   private AuthBundle.AuthBuilder<C> authBundleBuilder;
   private OpaBuilder<C> opaBundleBuilder;
   private CorsBundle.FinalBuilder<C> corsBundleBuilder;
   private ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder;
   private SwaggerBundle.FinalBuilder swaggerBundleBuilder;

   private SdaPlatformBundle(
         SecurityBundle.Builder securityBundleBuilder,
         JacksonConfigurationBundle.Builder jacksonConfigurationBundleBuilder,
         AuthBundle.AuthBuilder<C> authBundleBuilder,
         OpaBundle.OpaBuilder<C> opaBundleBuilder,
         CorsBundle.FinalBuilder<C> corsBundleBuilder,
         ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder,
         SwaggerBundle.FinalBuilder swaggerBundleBuilder) {
      this.securityBundleBuilder = securityBundleBuilder;
      this.jacksonConfigurationBundleBuilder = jacksonConfigurationBundleBuilder;
      this.authBundleBuilder = authBundleBuilder;
      this.opaBundleBuilder = opaBundleBuilder;
      this.corsBundleBuilder = corsBundleBuilder;
      this.consumerTokenBundleBuilder = consumerTokenBundleBuilder;
      this.swaggerBundleBuilder = swaggerBundleBuilder;
   }

   public static InitialPlatformBundleBuilder builder() {
      return new SdaPlatformBundle.Builder<>();
   }

   @Override
   public void initialize(Bootstrap<?> bootstrap) {

      // add normal bundles
      bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
      bootstrap.addBundle(DefaultLoggingConfigurationBundle.builder().build());
      bootstrap.addBundle(InternalHealthCheckEndpointBundle.builder().build());
      bootstrap.addBundle(PrometheusBundle.builder().build());
      bootstrap.addBundle(TraceTokenBundle.builder().build());

      // add configured bundles
      List<ConfiguredBundle<? super C>> configuredBundles = new ArrayList<>();
      configuredBundles.add(jacksonConfigurationBundleBuilder.build());
      configuredBundles.add(securityBundleBuilder.build());
      configuredBundles.add(swaggerBundleBuilder.build());
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

   public static class Builder<C extends Configuration> implements
         InitialPlatformBundleBuilder,
         AuthConfigProviderBuilder<C>,
         CorsConfigProviderBuilder<C>,
         ConsumerTokenConfigBuilder<C>,
         ConsumerTokenRequiredConfigBuilder<C>,
         SwaggerTitleBuilder<C>,
         SwaggerDetailsBuilder<C>,
         SwaggerFinalBuilder<C>,
         PlatformBundleBuilder<C> {

      private AuthBundle.AuthBuilder<C> authBundleBuilder;
      private OpaBundle.OpaBuilder<C> opaBundleBuilder;
      private ConsumerTokenConfig consumerTokenConfig;
      private ConsumerTokenBundle.FinalBuilder<C> consumerTokenBundleBuilder;
      private SecurityBundle.Builder securityBundleBuilder = SecurityBundle.builder();
      private JacksonConfigurationBundle.Builder jacksonBundleBuilder = JacksonConfigurationBundle.builder();
      private CorsBundle.FinalBuilder<C> corsBundleBuilder;

      private Info swaggerInfo = new Info();
      private boolean swaggerDisableEmbedParameter = false;
      private boolean swaggerDisableJsonExamples = false;
      private List<String> swaggerResourcePackages = new ArrayList<>();

      private Builder() {
      }


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
               createSwaggerBundleBuilder());
      }


      // InitialBuilder

      @Override
      public ConsumerTokenConfigBuilder<SdaPlatformConfiguration> usingSdaPlatformConfiguration() {
         return usingCustomConfig(SdaPlatformConfiguration.class)
               .withAuthConfigProvider(SdaPlatformConfiguration::getAuth)
               .withCorsConfigProvider(SdaPlatformConfiguration::getCors);
      }

      @Override
      public <T extends Configuration> AuthConfigProviderBuilder<T> usingCustomConfig(Class<T> configurationClass) {
         return new Builder<>();
      }


      // CustomConfigurationProviders and follow up configuration providers

      @Override
      public CorsConfigProviderBuilder<C> withoutAuthentication() {
         return this;
      }

      @Override
      public CorsConfigProviderBuilder<C> withAuthConfigProvider(AuthConfigProvider<C> authConfigProvider) {
         this.authBundleBuilder = AuthBundle.builder().withAuthConfigProvider(authConfigProvider).withAnnotatedAuthorization();
         return this;
      }

      @Override
      public CorsConfigProviderBuilder<C> withOpaAuthorization(
          AuthConfigProvider<C> authConfigProvider, OpaConfigProvider<C> opaConfigProvider) {
         this.authBundleBuilder = AuthBundle.builder().withAuthConfigProvider(authConfigProvider).withExternalAuthorization();
         this.opaBundleBuilder = OpaBundle.builder().withOpaConfigProvider(opaConfigProvider);
         return this;
      }

      @Override
      public ConsumerTokenConfigBuilder<C> withoutCorsSupport() {
         return this;
      }

      @Override
      public ConsumerTokenConfigBuilder<C> withCorsConfigProvider(CorsConfigProvider<C> corsConfigProvider) {
         this.corsBundleBuilder = CorsBundle.builder().withCorsConfigProvider(corsConfigProvider);
         return this;
      }

      @Override
      public SwaggerTitleBuilder<C> withoutConsumerTokenSupport() {
         return this;
      }

      @Override
      public SwaggerTitleBuilder<C> withOptionalConsumerToken() {
         consumerTokenConfig = new ConsumerTokenConfig();
         consumerTokenConfig.setOptional(true);
         this.consumerTokenBundleBuilder =
               ConsumerTokenBundle.builder().withConfigProvider(c -> consumerTokenConfig);
         return this;
      }

      @Override
      public ConsumerTokenRequiredConfigBuilder<C> withRequiredConsumerToken() {
         consumerTokenConfig = new ConsumerTokenConfig();
         consumerTokenConfig.setOptional(false);
         this.consumerTokenBundleBuilder =
               ConsumerTokenBundle.builder().withConfigProvider(c -> consumerTokenConfig);
         return this;
      }

      @Override
      public SwaggerTitleBuilder<C> withConsumerTokenConfigProvider(ConsumerTokenConfigProvider<C> consumerTokenConfigProvider) {
         this.consumerTokenBundleBuilder = ConsumerTokenBundle.builder()
               .withConfigProvider(consumerTokenConfigProvider);
         return this;
      }

      @Override
      public SwaggerTitleBuilder<C> withExcludePatternsForRequiredConsumerToken(String... regex) {
         if (this.consumerTokenConfig == null) {
            throw new IllegalStateException("ConsumerToken support can't be configured because it is disabled.");
         }
         this.consumerTokenConfig.getExcludePatterns().addAll(Arrays.asList(regex));
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoTitle(String title) {
         this.swaggerInfo.title(title);
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> disableSwaggerEmbedParameter() {
         this.swaggerDisableEmbedParameter = true;
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> disableSwaggerJsonExamples() {
         this.swaggerDisableJsonExamples = true;
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoVersion(String version) {
         this.swaggerInfo.version(version);
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoDescription(String description) {
         this.swaggerInfo.description(description);
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoTermsOfServiceUrl(String termsOfServiceUrl) {
         this.swaggerInfo.termsOfService(termsOfServiceUrl);
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoContact(String name, String email, String url) {
         this.swaggerInfo.setContact(new Contact().name(name).email(email).url(url));
         return this;
      }

      @Override
      public SwaggerDetailsBuilder<C> withSwaggerInfoLicense(String name, String url) {
         this.swaggerInfo.setLicense(new License().name(name).url(url));
         return this;
      }

      @Override
      public SwaggerFinalBuilder<C> addSwaggerResourcePackage(String resourcePackage) {
         this.swaggerResourcePackages.add(resourcePackage);
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
      public PlatformBundleBuilder<C> withCorsAdditionalAllowedHeaders(String... additionalAllowedHeaders) {
         validateConfigureCors();
         this.corsBundleBuilder.withAdditionalAllowedHeaders(additionalAllowedHeaders);
         return this;
      }

      @Override
      public PlatformBundleBuilder<C> withCorsAdditionalExposedHeaders(String... additionalExposedHeaders) {
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
      public PlatformBundleBuilder<C> withObjectMapperCustomization(Consumer<ObjectMapper> customizer) {
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


      // helper

      private SwaggerBundle.FinalBuilder createSwaggerBundleBuilder() {

         SwaggerBundle.InterimBuilder interimBuilder = SwaggerBundle.builder().withTitle(swaggerInfo.getTitle());
         SwaggerBundle.FinalBuilder finalBuilder = interimBuilder.addResourcePackage(swaggerResourcePackages.get(0));
         if (swaggerResourcePackages.size() > 1) {
            for (String swaggerResourcePackage : swaggerResourcePackages.subList(1, swaggerResourcePackages.size())) {
               finalBuilder = finalBuilder.addResourcePackage(swaggerResourcePackage);
            }
         }
         finalBuilder = applySwaggerInfo(finalBuilder);
         if (swaggerDisableEmbedParameter) {
            finalBuilder = finalBuilder.disableEmbedParameter();
         }
         if (swaggerDisableJsonExamples) {
            finalBuilder = finalBuilder.disableJsonExamples();
         }
         return finalBuilder;
      }

      private SwaggerBundle.FinalBuilder applySwaggerInfo(SwaggerBundle.FinalBuilder finalBuilder) {
         if (swaggerInfo.getVersion() != null) {
            finalBuilder = finalBuilder.withVersion(swaggerInfo.getVersion());
         }
         if (swaggerInfo.getDescription() != null) {
            finalBuilder = finalBuilder.withDescription(swaggerInfo.getDescription());
         }
         if (swaggerInfo.getTermsOfService() != null) {
            finalBuilder = finalBuilder.withTermsOfServiceUrl(swaggerInfo.getTermsOfService());
         }
         finalBuilder = applySwaggerContactInfo(finalBuilder);
         finalBuilder = applySwaggerLicenseInfo(finalBuilder);
         return finalBuilder;
      }

      private SwaggerBundle.FinalBuilder applySwaggerContactInfo(SwaggerBundle.FinalBuilder finalBuilder) {
         if (swaggerInfo.getContact() != null) {
            Contact c = swaggerInfo.getContact();
            if (c.getName() != null && c.getEmail() != null && c.getUrl() != null) {
               finalBuilder = finalBuilder.withContact(c.getName(), c.getEmail(), c.getUrl());
            }
            else if (c.getName() != null && c.getEmail() != null) {
               finalBuilder = finalBuilder.withContact(c.getName(), c.getEmail());
            }
            else if (c.getName() != null) {
               finalBuilder = finalBuilder.withContact(c.getName());
            }
         }
         return finalBuilder;
      }

      private SwaggerBundle.FinalBuilder applySwaggerLicenseInfo(SwaggerBundle.FinalBuilder finalBuilder) {
         if (swaggerInfo.getLicense() != null) {
            License l = swaggerInfo.getLicense();
            if (l.getName() != null && l.getUrl() != null) {
               finalBuilder = finalBuilder.withLicense(l.getName(), l.getUrl());
            }
            else if (l.getName() != null) {
               finalBuilder = finalBuilder.withLicense(l.getName());
            }
         }
         return finalBuilder;
      }

      private void validateConfigureCors() {
         if (this.corsBundleBuilder == null) {
            throw new IllegalStateException("Attempt to configure CORS details, but CORS is not active.");
         }
      }

   }

}
