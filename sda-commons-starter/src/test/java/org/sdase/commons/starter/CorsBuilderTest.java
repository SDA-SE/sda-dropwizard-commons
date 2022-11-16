package org.sdase.commons.starter;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.starter.test.BundleAssertion;

class CorsBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @BeforeEach
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  void defaultCorsSettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        CorsBundle.builder().withCorsConfigProvider(SdaPlatformConfiguration::getCors).build());
  }

  @Test
  void customCorsSettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
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

  @Test
  void noCorsConfigurationIfCorsDisabled() {
    assertThatCode(
            () ->
                SdaPlatformBundle.builder()
                    .usingCustomConfig(SdaPlatformConfiguration.class)
                    .withAuthConfigProvider(c -> new AuthConfig())
                    .withoutCorsSupport()
                    .addOpenApiResourcePackageClass(this.getClass())
                    .withCorsAdditionalExposedHeaders("x-foo"))
        .isInstanceOf(IllegalStateException.class);
  }
}
