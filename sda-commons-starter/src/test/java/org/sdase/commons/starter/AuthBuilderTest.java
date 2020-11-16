package org.sdase.commons.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.opa.config.OpaConfig;
import org.sdase.commons.server.opa.config.OpaConfigProvider;
import org.sdase.commons.starter.test.BundleAssertion;

public class AuthBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void defaultAuth() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder()
            .withAuthConfigProvider(SdaPlatformConfiguration::getAuth)
            .withAnnotatedAuthorization()
            .build());
  }

  @Test
  public void customAuthConfigProvider() {

    AuthConfigProvider<SdaPlatformConfiguration> acp = c -> new AuthConfig();

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withAuthConfigProvider(acp)
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder().withAuthConfigProvider(acp).withAnnotatedAuthorization().build());
  }

  @Test
  public void disableAuth() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withoutAuthentication()
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, AuthBundle.class);
  }

  @Test
  public void opaAuthorization() {

    AuthConfigProvider<SdaPlatformConfiguration> acp = c -> new AuthConfig();
    OpaConfigProvider<SdaPlatformConfiguration> ocp = c -> new OpaConfig();

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withOpaAuthorization(acp, ocp)
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        AuthBundle.builder().withAuthConfigProvider(acp).withExternalAuthorization().build());

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, OpaBundle.builder().withOpaConfigProvider(ocp).build());
  }
}
