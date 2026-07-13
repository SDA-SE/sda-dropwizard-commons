package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.Configuration;
import org.sdase.commons.server.dropwizard.bundles.CorsConfiguration;

public class CorsTestConfiguration extends Configuration {

  private CorsConfiguration cors = new CorsConfiguration();

  public CorsConfiguration getCors() {
    if (cors == null) {
      return new CorsConfiguration();
    }
    return cors;
  }

  public void setCors(CorsConfiguration cors) {
    this.cors = cors;
  }
}
