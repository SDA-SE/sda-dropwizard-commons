package org.sdase.commons.starter;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.auth.AuthBundle;
import org.sdase.commons.server.consumer.ConsumerTokenBundle;
import org.sdase.commons.server.cors.CorsBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.dropwizard.bundles.DefaultLoggingConfigurationBundle;
import org.sdase.commons.server.healthcheck.InternalHealthCheckEndpointBundle;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.server.opa.OpaBundle;
import org.sdase.commons.server.openapi.OpenApiBundle;
import org.sdase.commons.server.opentelemetry.OpenTelemetryBundle;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.security.SecurityBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;
import org.sdase.commons.starter.test.BundleAssertion;

public class SdaPlatformBundleBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void allBundlesRegistered() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    SoftAssertions.assertSoftly(
        softly -> {
          softly
              .assertThat(
                  bundleAssertion.getBundleOfType(bundle, ConfigurationSubstitutionBundle.class))
              .isNotNull();
          softly
              .assertThat(
                  bundleAssertion.getBundleOfType(bundle, DefaultLoggingConfigurationBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, JacksonConfigurationBundle.class))
              .isNotNull();
          softly
              .assertThat(
                  bundleAssertion.getBundleOfType(bundle, InternalHealthCheckEndpointBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, PrometheusBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, TraceTokenBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, SecurityBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, OpenApiBundle.class))
              .isNotNull();
          softly.assertThat(bundleAssertion.getBundleOfType(bundle, AuthBundle.class)).isNotNull();
          softly.assertThat(bundleAssertion.getBundleOfType(bundle, OpaBundle.class)).isNotNull();
          softly.assertThat(bundleAssertion.getBundleOfType(bundle, CorsBundle.class)).isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, OpenTelemetryBundle.class))
              .isNotNull();
          softly
              .assertThat(bundleAssertion.getBundleOfType(bundle, ConsumerTokenBundle.class))
              .isNotNull();
          softly.assertThat(bundleAssertion.countAddedBundles(bundle)).isEqualTo(13);
        });
  }
}
