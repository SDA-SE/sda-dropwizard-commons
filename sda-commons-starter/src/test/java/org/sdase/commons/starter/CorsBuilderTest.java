package org.sdase.commons.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.starter.test.BundleAssertion;

public class CorsBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void defaultCorsSettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        CorsBundle.builder().withCorsConfigProvider(SdaPlatformConfiguration::getCors).build());
  }

  @Test
  public void customCorsSettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .withCorsAdditionalExposedHeaders("X-custom-header-1", "X-custom-header-2")
            .withCorsAdditionalAllowedHeaders("X-custom-header-3", "X-custom-header-4")
            .withCorsAllowedMethods("GET")
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        CorsBundle.builder()
            .withCorsConfigProvider(SdaPlatformConfiguration::getCors)
            .withAdditionalExposedHeaders("X-custom-header-1", "X-custom-header-2")
            .withAdditionalAllowedHeaders("X-custom-header-3", "X-custom-header-4")
            .withAllowedMethods("GET")
            .build());
  }

  @Test(expected = IllegalStateException.class)
  public void noCorsConfigurationIfCorsDisabled() {
    SdaPlatformBundle.builder()
        .usingCustomConfig(SdaPlatformConfiguration.class)
        .withAuthConfigProvider(c -> new AuthConfig())
        .withoutCorsSupport()
        .withoutConsumerTokenSupport()
        .addOpenApiResourcePackageClass(this.getClass())
        .withCorsAdditionalExposedHeaders("x-foo");
  }
}
