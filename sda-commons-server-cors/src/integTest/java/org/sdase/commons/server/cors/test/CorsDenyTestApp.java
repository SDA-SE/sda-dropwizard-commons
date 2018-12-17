package org.sdase.commons.server.cors.test;

import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.server.cors.CorsBundle;

public class CorsDenyTestApp extends CorsTestApp {

   public static void main(String[] args) throws Exception {
      new CorsDenyTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
      bootstrap.addBundle(CorsBundle.builder()
            .withCorsConfigProvider(CorsTestConfiguration::getCors)
            .build());
   }

}
