package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.setup.Bootstrap;
import jakarta.ws.rs.HttpMethod;
import org.sdase.commons.server.dropwizard.bundles.CorsBundle;

public class CorsRestrictedTestApp extends CorsTestApp {

  public static void main(String[] args) throws Exception {
    new CorsRestrictedTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
    bootstrap.addBundle(
        CorsBundle.builder()
            .withCorsConfigProvider(CorsTestConfiguration::getCors)
            .withAdditionalAllowedHeaders("some")
            .withAdditionalExposedHeaders("exposed")
            .withAllowedMethods(HttpMethod.GET, HttpMethod.POST)
            .build());
  }
}
