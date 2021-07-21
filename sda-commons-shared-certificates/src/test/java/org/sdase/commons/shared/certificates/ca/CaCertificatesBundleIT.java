package org.sdase.commons.shared.certificates.ca;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ConfigOverride.randomPorts;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.DropwizardTestSupport;
import java.nio.file.Paths;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CaCertificatesBundleIT {

  private static DropwizardTestSupport<CaCertificateTestConfiguration> DW;

  @BeforeAll
  static void setUp() throws Exception {
    DW =
        new DropwizardTestSupport<>(
            CaCertificateTestApp.class,
            null,
            randomPorts(),
            config(
                "config.customCaCertificateDir", Paths.get("src", "test", "resources").toString()));
    DW.before();
  }

  @Test
  void shouldCreatSslContext() {
    SSLContext sslContext =
        DW.<CaCertificateTestApp>getApplication().getCaCertificatesBundle().getSslContext();

    assertThat(sslContext).isNotNull().extracting(SSLContext::getProtocol).isEqualTo("TLSv1.2");
  }

  public static class CaCertificateTestApp extends Application<CaCertificateTestConfiguration> {

    private final CaCertificatesBundle<CaCertificateTestConfiguration> caCertificatesBundle =
        CaCertificatesBundle.builder()
            .withCaCertificateConfigProvider(CaCertificateTestConfiguration::getConfig)
            .build();

    @Override
    public void initialize(Bootstrap<CaCertificateTestConfiguration> bootstrap) {
      bootstrap.addBundle(caCertificatesBundle);
    }

    public CaCertificatesBundle<CaCertificateTestConfiguration> getCaCertificatesBundle() {
      return caCertificatesBundle;
    }

    @Override
    public void run(CaCertificateTestConfiguration configuration, Environment environment) {}
  }
}
