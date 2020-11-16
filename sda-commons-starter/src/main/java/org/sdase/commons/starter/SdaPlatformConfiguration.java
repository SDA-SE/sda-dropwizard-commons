package org.sdase.commons.starter;

import io.dropwizard.Configuration;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.consumer.ConsumerTokenConfig;
import org.sdase.commons.server.cors.CorsConfiguration;
import org.sdase.commons.starter.builder.CustomConfigurationProviders.ConsumerTokenConfigBuilder;

/** Default configuration for the {@link SdaPlatformBundle}. */
public class SdaPlatformConfiguration extends Configuration {

  /** Configuration of authentication. */
  private AuthConfig auth = new AuthConfig();

  /** Configuration of the CORS filter. */
  private CorsConfiguration cors = new CorsConfiguration();

  /**
   * Configuration of the consumer token. This configuration only affects the consumer token bundle,
   * if it is explicitly added using {@link
   * ConsumerTokenConfigBuilder#withConsumerTokenConfigProvider(ConsumerTokenBundle.ConsumerTokenConfigProvider)}:
   *
   * <pre>{@code
   * SdaPlatformBundle<SdaPlatformConfiguration> bundle =
   *     SdaPlatformBundle.builder()
   *         .usingSdaPlatformConfiguration()
   *         .withConsumerTokenConfigProvider(SdaPlatformConfiguration::getConsumerToken)
   *         // ...
   * }</pre>
   */
  private ConsumerTokenConfig consumerToken = new ConsumerTokenConfig();

  public AuthConfig getAuth() {
    return auth;
  }

  public SdaPlatformConfiguration setAuth(AuthConfig auth) {
    this.auth = auth;
    return this;
  }

  public CorsConfiguration getCors() {
    return cors;
  }

  public SdaPlatformConfiguration setCors(CorsConfiguration cors) {
    this.cors = cors;
    return this;
  }

  public ConsumerTokenConfig getConsumerToken() {
    return consumerToken;
  }

  public SdaPlatformConfiguration setConsumerToken(ConsumerTokenConfig consumerToken) {
    this.consumerToken = consumerToken;
    return this;
  }
}
