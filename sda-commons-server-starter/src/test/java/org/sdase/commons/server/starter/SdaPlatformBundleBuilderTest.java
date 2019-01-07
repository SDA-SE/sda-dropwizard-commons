package org.sdase.commons.server.starter;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.server.starter.test.BundleAssertion;
import org.sdase.commons.server.swagger.SwaggerBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

public class SdaPlatformBundleBuilderTest {

   private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

   @Before
   public void setUp() {
      bundleAssertion = new BundleAssertion<>();
   }

   @Test
   public void allBundlesRegistered() {

      SdaPlatformBundle<SdaPlatformConfiguration> bundle = SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withOptionalConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

      SoftAssertions.assertSoftly(softly -> {
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, ConfigurationSubstitutionBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, JacksonConfigurationBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, PrometheusBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, TraceTokenBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, SecurityBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, SwaggerBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, AuthBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, CorsBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.getBundleOfType(bundle, ConsumerTokenBundle.class)).isNotNull();
         softly.assertThat(bundleAssertion.countAddedBundles(bundle)).isEqualTo(9);
      });


   }

}
