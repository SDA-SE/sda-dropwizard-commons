package org.sdase.commons.server.s3;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import java.util.List;
import org.junit.jupiter.api.Test;

class S3BundleProxyConfigurationTest {

  @Test
  void shouldCreateProxyConfigurationWithNonProxyHosts() {
    // given
    var s3Config =
        new S3Configuration()
            .setNonProxyHosts(List.of(" localhost ", "*.internal.example.com|api.local"));
    var s3Bundle = createS3BundleWithConfig(s3Config);

    // when
    var proxyConfiguration = s3Bundle.createProxyConfiguration(s3Config);

    // then
    assertThat(proxyConfiguration).isNotNull();
    assertThat(proxyConfiguration.nonProxyHosts())
        .containsExactlyInAnyOrder("localhost", ".*?.internal.example.com", "api.local");
    assertThat(proxyConfiguration.host()).isNull();
    assertThat(proxyConfiguration.port()).isZero();
  }

  @Test
  void shouldIgnoreEmptyNonProxyHosts() {
    // given
    var s3Config = new S3Configuration().setNonProxyHosts(List.of(" ", ""));
    var s3Bundle = createS3BundleWithConfig(s3Config);

    // when
    var proxyConfiguration = s3Bundle.createProxyConfiguration(s3Config);

    // then
    assertThat(proxyConfiguration).isNull();
  }

  private static S3Bundle<Configuration> createS3BundleWithConfig(S3Configuration s3Config) {
    return S3Bundle.builder().withConfigurationProvider(configuration -> s3Config).build();
  }
}
