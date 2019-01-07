package org.sdase.commons.server.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.auth.config.AuthConfig;
import org.sdase.commons.server.auth.config.AuthConfigProvider;
import org.sdase.commons.server.starter.test.BundleAssertion;

public class AuthBuilderTest {

   private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

   @Before
   public void setUp() {
      bundleAssertion = new BundleAssertion<>();
   }

   @Test
   public void defaultAuth() {
      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("Starter") // NOSONAR
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      bundleAssertion.assertBundleConfiguredByPlatformBundle(
            bundle,
            AuthBundle.builder().withAuthConfigProvider(SdaPlatformConfiguration::getAuth).build()
      );
   }

   @Test
   public void customAuthConfigProvider() {

      AuthConfigProvider<SdaPlatformConfiguration> acp = c -> new AuthConfig();

      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withAuthConfigProvider(acp)
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      bundleAssertion.assertBundleConfiguredByPlatformBundle(
            bundle,
            AuthBundle.builder().withAuthConfigProvider(acp).build()
      );
   }

   @Test
   public void disableAuth() {

      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingCustomConfig(SdaPlatformConfiguration.class)
            .withoutAuthentication()
            .withoutCorsSupport()
            .withoutConsumerTokenSupport()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, AuthBundle.class);
   }
}
