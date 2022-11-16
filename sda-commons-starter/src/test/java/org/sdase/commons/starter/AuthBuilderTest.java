package org.sdase.commons.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.starter.test.BundleAssertion;

class AuthBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @BeforeEach
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  void defaultAuth() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder()
            .withAuthConfigProvider(SdaPlatformConfiguration::getAuth)
            .withExternalAuthorization()
            .build());
  }

  @Test
  void defaultOpa() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        OpaBundle.builder().withOpaConfigProvider(SdaPlatformConfiguration::getOpa).build());
  }

  @Test
  void customAuthConfigProvider() {

    AuthConfigProvider<SdaPlatformConfiguration> acp = c -> new AuthConfig();

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withAuthConfigProvider(acp)
            .withoutCorsSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder().withAuthConfigProvider(acp).withAnnotatedAuthorization().build());
  }

  @Test
  void disableAuth() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withoutAuthentication()
            .withoutCorsSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, AuthBundle.class);
  }

  @Test
  void opaAuthorization() {

    AuthConfigProvider<SdaPlatformConfiguration> acp = c -> new AuthConfig();
    OpaConfigProvider<SdaPlatformConfiguration> ocp = c -> new OpaConfig();

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withOpaAuthorization(acp, ocp)
            .withoutCorsSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder().withAuthConfigProvider(acp).withExternalAuthorization().build());

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, OpaBundle.builder().withOpaConfigProvider(ocp).build());
  }
}
