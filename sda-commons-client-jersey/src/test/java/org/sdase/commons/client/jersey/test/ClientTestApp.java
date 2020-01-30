package org.sdase.commons.client.jersey.test;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.Application;
import io.dropwizard.forms.MultiPartBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.client.jersey.JerseyClientBundle;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;
import org.sdase.commons.server.trace.TraceTokenBundle;

public class ClientTestApp extends Application<ClientTestConfig> {

  private JerseyClientBundle<ClientTestConfig> jerseyClientBundle =
      JerseyClientBundle.builder()
          .withConsumerTokenProvider(ClientTestConfig::getConsumerToken)
          .build();

  public static void main(String[] args) throws Exception {
    new ClientTestApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<ClientTestConfig> bootstrap) {
    bootstrap.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(TraceTokenBundle.builder().build());
    bootstrap.addBundle(jerseyClientBundle);
    bootstrap.addBundle(new MultiPartBundle());
  }

  @Override
  public void run(ClientTestConfig configuration, Environment environment) {
    environment.jersey().register(this);
    environment
        .jersey()
        .register(
            new ClientTestEndPoint(
                jerseyClientBundle.getClientFactory(), configuration.getMockBaseUrl()));
  }

  public JerseyClientBundle getJerseyClientBundle() {
    return jerseyClientBundle;
  }
}
