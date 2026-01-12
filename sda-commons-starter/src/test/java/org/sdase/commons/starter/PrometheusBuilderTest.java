package org.sdase.commons.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.prometheus.PrometheusBundle;
import org.sdase.commons.server.prometheus.config.PrometheusConfiguration;
import org.sdase.commons.starter.test.BundleAssertion;

class PrometheusBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @BeforeEach
  void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  void withDefaultPrometheusConfiguration() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withDefaultPrometheusConfig()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, PrometheusBundle.builder().withDefaultPrometheusConfig().build());
  }

  @Test
  void withCustomPrometheusConfiguration() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withPrometheusConfigProvider(
                c -> {
                  PrometheusConfiguration pc = new PrometheusConfiguration();
                  pc.setEnableRequestHistogram(true);
                  return pc;
                })
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        PrometheusBundle.builder()
            .withConfigurationProvider(
                c -> {
                  PrometheusConfiguration pc = new PrometheusConfiguration();
                  pc.setEnableRequestHistogram(true);
                  return pc;
                })
            .build());
  }

  @Test
  void withDefaultPrometheusConfigurationWithoutConfiguration() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder().usingSdaPlatformConfiguration().build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, PrometheusBundle.builder().withDefaultPrometheusConfig().build());
  }
}
