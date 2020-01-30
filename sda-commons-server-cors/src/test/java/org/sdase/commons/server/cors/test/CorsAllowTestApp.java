package org.sdase.commons.server.cors.test;

import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.server.cors.CorsBundle;

public class CorsAllowTestApp extends CorsTestApp {

  public static void main(String[] args) throws Exception {
    new CorsAllowTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
    bootstrap.addBundle(
        CorsBundle.builder()
            .withCorsConfigProvider(CorsTestConfiguration::getCors)
            .withAdditionalAllowedHeaders("some")
            .withAdditionalExposedHeaders("exposed")
            .build());
  }
}
