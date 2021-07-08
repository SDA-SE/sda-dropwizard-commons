package org.sdase.commons.shared.certificates.ca;

import io.dropwizard.Configuration;
import io.dropwizard.ConfiguredBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class CaCertificatesBundle<C extends Configuration> implements ConfiguredBundle<C> {

  private static final String DEFAULT_TRUSTED_CERTIFICATES_DIR = "/var/trust/certificates";

  @Override
  public void initialize(Bootstrap<?> bootstrap) {
    // maybe nothing to do here
  }

  @Override
  public void run(C configuration, Environment environment) {
    // read the certificates
  }
}
