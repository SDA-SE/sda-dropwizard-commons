package org.sdase.commons.shared.certificates.ca;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;

class CaCertificatesBundleTest {

  @Test
  void doNotAccessSslContextBeforeRun() {

    CaCertificatesBundle<CaCertificateTestConfiguration> caCertificatesBundle =
        CaCertificatesBundle.builder()
            .withCaCertificateConfigProvider(CaCertificateTestConfiguration::getConfig)
            .build();

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(caCertificatesBundle::getSslContext);
  }
}
