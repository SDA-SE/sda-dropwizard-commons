package org.sdase.commons.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.starter.test.BundleAssertion;

public class SecurityBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void defaultSecuritySettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, SecurityBundle.builder().build());
  }

  @Test
  public void customizableLimitsSecuritySettings() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .disableBufferLimitValidationSecurityFeature()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, SecurityBundle.builder().disableBufferLimitValidation().build());
  }
}
