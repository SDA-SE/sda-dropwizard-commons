package org.sdase.commons.server.starter.builder;

import io.dropwizard.Configuration;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiResponse;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.consumer.ConsumerTokenBundle.ConsumerTokenConfigProvider;
import org.sdase.commons.server.cors.CorsConfigProvider;
import org.sdase.commons.server.opa.config.OpaConfigProvider;

/**
 * Container for builder interfaces required for custom configuration classes. They are all in one
 * place in the order how they are called for easier extension with more supported bundles.
 */
public interface CustomConfigurationProviders {

  interface AuthConfigProviderBuilder<C extends Configuration> {
    /**
     * Disable authentication entirely.
     *
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withoutAuthentication();

    /**
     * Enables authentication (i.e. token validation) for annotated endpoints and requires tokens
     *
     * @param authConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.auth.config.AuthConfig}, e.g. {@code MyAppConfig::getAuth}
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withAuthConfigProvider(AuthConfigProvider<C> authConfigProvider);

    /**
     * Enables authentication (i.e. token validation) for all endpoints and use the {@link
     * org.sdase.commons.server.opa.OpaBundle} to authorize the requests. Requests without
     * Authorization header will <i>not</i> be rejected but need to be handled in the Authorization
     * policy.
     *
     * @param authConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.auth.config.AuthConfig}, e.g. {@code MyAppConfig::getAuth}
     * @param opaConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.opa.config.OpaConfig}, e.g. {@code MyAppConfig::getOpa}
     * @return the builder instance
     */
    CorsConfigProviderBuilder<C> withOpaAuthorization(
        AuthConfigProvider<C> authConfigProvider, OpaConfigProvider<C> opaConfigProvider);
  }

  interface CorsConfigProviderBuilder<C extends Configuration> {

    /**
     * Disable CORS support. Browsers are not allowed to access the application from a foreign
     * domain.
     *
     * @return the builder instance
     */
    ConsumerTokenConfigBuilder<C> withoutCorsSupport();

    /**
     * @param corsConfigProvider a provider, for the {@link
     *     org.sdase.commons.server.cors.CorsConfiguration}, e.g. {@code MyAppConfig::getCors}
     * @return the builder instance
     */
    ConsumerTokenConfigBuilder<C> withCorsConfigProvider(CorsConfigProvider<C> corsConfigProvider);
  }

  interface ConsumerTokenConfigBuilder<C extends Configuration> {

    /**
     * Disable consumer token support. Consumer tokens are not required to access this service and
     * will be ignored if a client sends them.
     *
     * @return the builder instance
     */
    SwaggerTitleBuilder<C> withoutConsumerTokenSupport();

    /**
     * Disable consumer token support. Consumer tokens are not required to access this service but
     * will be tracked if a client sends them.
     *
     * @return the builder instance
     */
    SwaggerTitleBuilder<C> withOptionalConsumerToken();

    /**
     * Enable consumer token support. Consumer tokens are required to access this service. Further
     * configuration may exclude paths from this requirement.
     *
     * @return the builder instance
     */
    ConsumerTokenRequiredConfigBuilder<C> withRequiredConsumerToken();

    /**
     * Configure consumer token support based on provided configuration per instance.
     *
     * @param consumerTokenConfigProvider the provider of the configuration, e.g. {@code
     *     MyConfig::getConsumerToken}
     * @return the builder instance
     */
    SwaggerTitleBuilder<C> withConsumerTokenConfigProvider(
        ConsumerTokenConfigProvider<C> consumerTokenConfigProvider);
  }

  interface ConsumerTokenRequiredConfigBuilder<C extends Configuration>
      extends SwaggerTitleBuilder<C> {

    /**
     * Define paths by regex patterns that do not require a consumer token from the client.
     *
     * @param regex regex that matches paths that do not require a consumer token from the client
     * @return the builder instance
     */
    SwaggerTitleBuilder<C> withExcludePatternsForRequiredConsumerToken(String... regex);
  }

  interface SwaggerTitleBuilder<C extends Configuration> {

    /**
     * Disables the Swagger support for this application. This is useful if the {@link
     * org.sdase.commons.server.swagger.SwaggerBundle} should be configured manually of if the
     * OpenApiBundle is used instead.
     *
     * @return the builder
     */
    PlatformBundleBuilder<C> withoutSwagger();

    /**
     * Sets the title of the API in the swagger documentation.
     *
     * @param title the title; not null or empty
     * @return the builder
     * @throws NullPointerException if title is null
     * @throws IllegalArgumentException if title is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoTitle(String title);
  }

  interface SwaggerDetailsBuilder<C extends Configuration> extends SwaggerScanningBuilder<C> {
    /**
     * Disables automatic addition of the embed query parameter if embeddable resources are
     * discovered.
     *
     * @return the builder.
     */
    SwaggerDetailsBuilder<C> disableSwaggerEmbedParameter();

    /**
     * Disables automatic rendering of Json examples in Swagger {@link ApiModelProperty#example()
     * property examples} and {@link ApiResponse#examples() response examples}. If disabled, only
     * {@code String} and {@link Integer} are recognized as special types.
     *
     * @return the builder
     */
    SwaggerDetailsBuilder<C> disableSwaggerJsonExamples();

    /**
     * Sets the version of the API.
     *
     * <p>Note: If no version is given (i.e. this method is not used) {@code 1.0} is used.
     *
     * @param version the version; not null or empty
     * @return the builder
     * @throws NullPointerException if version is null
     * @throws IllegalArgumentException if version is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoVersion(String version);

    /**
     * Sets the description of the API.
     *
     * @param description the description; not null or empty
     * @return the builder
     * @throws NullPointerException if description is null
     * @throws IllegalArgumentException if description is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoDescription(String description);

    /**
     * Sets the Terms of Service URL of the API.
     *
     * @param termsOfServiceUrl the Terms of Service URL; not null or empty
     * @return the builder
     * @throws NullPointerException if termsOfServiceUrl is null
     * @throws IllegalArgumentException if termsOfServiceUrl is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoTermsOfServiceUrl(String termsOfServiceUrl);

    /**
     * Sets the contact of the API.
     *
     * @param name the contact's name; not null or empty
     * @return the builder
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    default SwaggerDetailsBuilder<C> withSwaggerInfoContact(String name) {
      return withSwaggerInfoContact(name, null, null);
    }

    /**
     * Sets the contact of the API.
     *
     * @param name the contact's name; not null or empty
     * @param email the contact's email; not null or empty
     * @return the builder
     * @throws NullPointerException if name or email is null
     * @throws IllegalArgumentException if name or email is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    default SwaggerDetailsBuilder<C> withSwaggerInfoContact(String name, String email) {
      return withSwaggerInfoContact(name, email, null);
    }

    /**
     * Sets the contact of the API.
     *
     * @param name the contact's name; not null or empty
     * @param email the contact's email; not null or empty
     * @param url the contact's url; not null or empty
     * @return the builder
     * @throws NullPointerException if name, email, or url is null
     * @throws IllegalArgumentException if name, email, or url is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoContact(String name, String email, String url);

    /**
     * Sets the license of the API.
     *
     * @param name the license's name; not null or empty
     * @return the builder
     * @throws NullPointerException if name is null
     * @throws IllegalArgumentException if name is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    default SwaggerDetailsBuilder<C> withSwaggerInfoLicense(String name) {
      return withSwaggerInfoLicense(name, null);
    }

    /**
     * Sets the license of the API.
     *
     * @param name the license's name; not null or empty
     * @param url the license's url; not null or empty
     * @return the builder
     * @throws NullPointerException if name or url is null
     * @throws IllegalArgumentException if name or url is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerDetailsBuilder<C> withSwaggerInfoLicense(String name, String url);
  }

  interface SwaggerScanningBuilder<C extends Configuration> {

    /**
     * Adds a package to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackage the package to be scanned; not null
     * @return the builder
     * @throws NullPointerException if resourcePackage is null
     * @throws IllegalArgumentException if resourcePackage is empty
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    SwaggerFinalBuilder<C> addSwaggerResourcePackage(String resourcePackage);

    /**
     * Adds the package of the given class to the packages Swagger should scan to pick up resources.
     *
     * @param resourcePackageClass the class whose package should be scanned; not null
     * @return the builder
     * @throws NullPointerException if resourcePackageClass is null
     * @see <a
     *     href="https://github.com/swagger-api/swagger-core/wiki/Swagger-Core-RESTEasy-2.X-Project-Setup#using-swaggers-beanconfig">Swagger's
     *     BeanConfig</a>
     */
    default SwaggerFinalBuilder<C> addSwaggerResourcePackageClass(Class<?> resourcePackageClass) {
      return addSwaggerResourcePackage(resourcePackageClass.getPackage().getName());
    }
  }

  interface SwaggerFinalBuilder<C extends Configuration>
      extends SwaggerScanningBuilder<C>, PlatformBundleBuilder<C> {}
}
