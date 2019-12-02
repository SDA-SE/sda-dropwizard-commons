package org.sdase.commons.server.cors.test;

import io.dropwizard.setup.Bootstrap;
import org.sdase.commons.server.cors.CorsBundle;

import javax.ws.rs.HttpMethod;

public class CorsRestrictedTestApp extends CorsTestApp {

   public static void main(String[] args) throws Exception {
      new CorsRestrictedTestApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<CorsTestConfiguration> bootstrap) {
      bootstrap.addBundle(CorsBundle.builder()
            .withCorsConfigProvider(CorsTestConfiguration::getCors)
            .withAdditionalAllowedHeaders("some")
            .withAdditionalExposedHeaders("exposed")
            .withAllowedMethods(HttpMethod.GET, HttpMethod.POST)
            .build());
   }

}
