package org.sdase.commons.shared.certificates.ca;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.security.KeyStoreException;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import org.apache.commons.lang3.StringUtils;
import org.sdase.commons.shared.certificates.ca.ssl.CertificateReader;
import org.sdase.commons.shared.certificates.ca.ssl.SslUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CaCertificatesBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CaCertificatesBundle.class);

  public static final String DEFAULT_TRUSTED_CERTIFICATES_DIR = "/var/trust/certificates";
  private final CaCertificateConfigurationProvider<C> configProvider;
  private CertificateReader certificateReader =
      new CertificateReader(DEFAULT_TRUSTED_CERTIFICATES_DIR);
  private SSLContext sslContext;
  private boolean isCertificateLoaded = false;

  public CaCertificatesBundle(CaCertificateConfigurationProvider<C> configProvider) {
    this.configProvider = configProvider;
  }

  public static <T extends Configuration> Builder<T> builder() {
    return new Builder<>();
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // maybe nothing to do here
  }

  @Override
  public void run(C configuration, Environment environment) throws KeyStoreException {
    String processedDirectory = DEFAULT_TRUSTED_CERTIFICATES_DIR;
    if (configProvider != null) {
      CaCertificateConfiguration certificateConfiguration = configProvider.apply(configuration);
      if (certificateConfiguration != null
          && StringUtils.isNotBlank(certificateConfiguration.getCustomCaCertificateDir())) {
        processedDirectory = certificateConfiguration.getCustomCaCertificateDir();
        this.certificateReader = new CertificateReader(processedDirectory);
      }
    }
    // read the certificates
    Optional<SSLContext> optionalSSLContext = createSSLContext();
    if (optionalSSLContext.isPresent()) {
      this.sslContext = optionalSSLContext.get();
      LOGGER.info("Loaded certificates from {}", processedDirectory);
    } else {
      LOGGER.warn("No certificates are found in the provided directory {}", processedDirectory);
    }
    isCertificateLoaded = true;
  }

  /**
   * @return the created {@link SSLContext} that is ready to be used in other modules. If the
   *     returned value is null then there was no certificates found in the provided config
   *     directory.
   * @throws IllegalStateException if the method is called before the sslContext is initialized in
   *     {@link #run(Configuration, Environment)}
   */
  @Nullable
  public SSLContext getSslContext() {
    if (sslContext == null && !isCertificateLoaded) {
      throw new IllegalStateException(
          "Could not access sslContext before Application#run(Configuration, Environment).");
    }
    return sslContext;
  }

  private Optional<SSLContext> createSSLContext() {
    return certificateReader
        .readCertificates() // pem content as strings
        .map(SslUtil::createTruststoreFromPemKey) // a keystore instance that have certs loaded
        .map(SslUtil::createSslContext); // the sslContext created with the previous keystore
  }

  public interface InitialBuilder {
    <C extends Configuration> FinalBuilder<C> withCaCertificateConfigProvider(
        CaCertificateConfigurationProvider<C> configProvider);
  }

  public interface FinalBuilder<C extends Configuration> {
    CaCertificatesBundle<C> build();
  }

  public static class Builder<C extends Configuration> implements InitialBuilder, FinalBuilder<C> {

    private CaCertificateConfigurationProvider<C> configProvider;

    private Builder() {}

    private Builder(CaCertificateConfigurationProvider<C> configProvider) {
      this.configProvider = configProvider;
    }

    @Override
    public <C1 extends Configuration> FinalBuilder<C1> withCaCertificateConfigProvider(
        CaCertificateConfigurationProvider<C1> configProvider) {
      return new Builder<>(configProvider);
    }

    @Override
    public CaCertificatesBundle<C> build() {
      return new CaCertificatesBundle<>(configProvider);
    }
  }
}
