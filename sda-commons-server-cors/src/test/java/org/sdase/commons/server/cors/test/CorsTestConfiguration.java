package org.sdase.commons.server.cors.test;

import io.dropwizard.Configuration;
import org.sdase.commons.server.cors.CorsConfiguration;

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
