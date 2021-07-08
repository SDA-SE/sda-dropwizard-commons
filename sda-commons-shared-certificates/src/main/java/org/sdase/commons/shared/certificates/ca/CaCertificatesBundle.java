package org.sdase.commons.shared.certificates.ca;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import java.util.function.Function;

public class CaCertificatesBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final String DEFAULT_TRUSTED_CERTIFICATES_DIR = "/var/trust/certificates";

  private final TrustedCertificatesDirConfigProvider<C> configProvider;

  public CaCertificatesBundle(TrustedCertificatesDirConfigProvider<C> configProvider) {
    this.configProvider = configProvider;
  }

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // maybe nothing to do here
  }

  @Override
  public void run(C configuration, Environment environment) {
    final String certificateDir = configProvider.apply(configuration);
    // read the certificates
  }

  public static <C extends Configuration> InitialBuilder<C> builder() {
    return new Builder<>();
  }

  public interface InitialBuilder<C1 extends Configuration> extends FinalBuilder<C1> {
    FinalBuilder<C1> withTrustedCertificatesDir(
        TrustedCertificatesDirConfigProvider<C1> configProvider);
  }

  public interface FinalBuilder<C extends Configuration> {
    CaCertificatesBundle<C> build();
  }

  public static class Builder<C extends Configuration>
      implements InitialBuilder<C>, FinalBuilder<C> {

    private TrustedCertificatesDirConfigProvider<C> trustedCertificatesDir =
        c -> DEFAULT_TRUSTED_CERTIFICATES_DIR;

    @Override
    public FinalBuilder<C> withTrustedCertificatesDir(
        TrustedCertificatesDirConfigProvider<C> configProvider) {
      this.trustedCertificatesDir = configProvider;
      return this;
    }

    @Override
    public CaCertificatesBundle<C> build() {
      return new CaCertificatesBundle<>(trustedCertificatesDir);
    }
  }

  /** @param <C> the type of the specific {@link Configuration} used in the application */
  @FunctionalInterface
  public interface TrustedCertificatesDirConfigProvider<C extends Configuration>
      extends Function<C, String> {}
}
