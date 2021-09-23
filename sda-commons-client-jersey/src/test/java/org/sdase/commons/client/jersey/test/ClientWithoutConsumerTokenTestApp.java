package org.sdase.commons.client.jersey.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.client.jersey.oidc.filter.OidcRequestFilter;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

public class ClientWithoutConsumerTokenTestApp extends Application<ClientTestConfig> {

  private JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().build();

  private ClientTestEndPoint clientTestEndPoint;

  public static void main(String[] args) throws Exception {
    new ClientWithoutConsumerTokenTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<ClientTestConfig> bootstrap) {
    bootstrap.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(TraceTokenBundle.builder().build());
    bootstrap.addBundle(jerseyClientBundle);
  }

  @Override
  public void run(ClientTestConfig configuration, Environment environment) {
    OidcRequestFilter oidcRequestFilter =
        new OidcRequestFilter(jerseyClientBundle.getClientFactory(), configuration.getOidc(), true);
    clientTestEndPoint =
        new ClientTestEndPoint(
            jerseyClientBundle.getClientFactory(),
            configuration.getMockBaseUrl(),
            oidcRequestFilter);
    environment.jersey().register(this);
    environment.jersey().register(clientTestEndPoint);
  }

  public JerseyClientBundle getJerseyClientBundle() {
    return jerseyClientBundle;
  }

  public ClientTestEndPoint getClientTestEndPoint() {
    return clientTestEndPoint;
  }
}
