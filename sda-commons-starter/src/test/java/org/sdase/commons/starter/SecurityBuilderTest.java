package org.sdase.commons.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.starter.test.BundleAssertion;

class SecurityBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @BeforeEach
  void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  void defaultSecuritySettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, SecurityBundle.builder().build());
  }

  @Test
  void customizableLimitsSecuritySettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .disableBufferLimitValidationSecurityFeature()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, SecurityBundle.builder().disableBufferLimitValidation().build());
  }

  @Test
  void frontendSupportSecuritySettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .withFrontendSupport()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, SecurityBundle.builder().withFrontendSupport().build());
  }
}
