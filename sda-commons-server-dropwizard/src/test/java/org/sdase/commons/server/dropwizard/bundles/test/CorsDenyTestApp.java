package org.sdase.commons.server.dropwizard.bundles.test;

import io.dropwizard.core.setup.Bootstrap;
import org.sdase.commons.server.dropwizard.bundles.CorsBundle;

public class CorsDenyTestApp extends CorsTestApp {

  public static void main(String[] args) throws Exception {
    new CorsDenyTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
    bootstrap.addBundle(
        CorsBundle.builder().withCorsConfigProvider(CorsTestConfiguration::getCors).build());
  }
}
