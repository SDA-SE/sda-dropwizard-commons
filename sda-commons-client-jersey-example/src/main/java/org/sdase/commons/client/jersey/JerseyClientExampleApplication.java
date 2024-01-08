package org.sdase.commons.client.jersey;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import java.net.URI;
import java.util.List;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.sdase.commons.client.jersey.clients.apia.ApiA;
import org.sdase.commons.client.jersey.clients.apia.Car;
import org.sdase.commons.server.dropwizard.bundles.ConfigurationSubstitutionBundle;

public class JerseyClientExampleApplication extends Application<JerseyClientExampleConfiguration> {

  public static final String CARS_PATH = URI.create("cars").toString();
  private final JerseyClientBundle<Configuration> jerseyClientBundle =
      JerseyClientBundle.builder().withConsumerTokenProvider(c -> "MyConsumerToken").build();

  private Client externalClient;
  private Client platformClient;
  private ApiA apiClient;
  private String apiABaseUrl;
  private Client configuredExternalClient;

  public static void main(String[] args) throws Exception {
    new JerseyClientExampleApplication().run(args);
  }

  @Override
  public void initialize(Bootstrap<JerseyClientExampleConfiguration> bootstrap) {
    bootstrap.getObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    bootstrap.addBundle(ConfigurationSubstitutionBundle.builder().build());
    bootstrap.addBundle(jerseyClientBundle);
  }

  @Override
  public void run(JerseyClientExampleConfiguration configuration, Environment environment) {
    apiABaseUrl = configuration.getServicea();

    // Example 1:
    // create a platform client that can pass trough authentication, trace token and consumer token
    platformClient =
        jerseyClientBundle
            .getClientFactory()
            .platformClient()
            .enableAuthenticationPassThrough()
            .enableConsumerToken()
            .buildGenericClient("platformClient");

    // Example 2:
    // create a external client that calls an external API
    externalClient =
        jerseyClientBundle.getClientFactory().externalClient().buildGenericClient("externalClient");

    // Example 3:
    // create an external client based on an API that can simply be used as java interface
    // (basic authentication header is set automatically)
    apiClient =
        jerseyClientBundle
            .getClientFactory()
            .externalClient()
            .addFeature(HttpAuthenticationFeature.basic("foo", "bar"))
            .api(ApiA.class)
            .atTarget(apiABaseUrl);

    // Example 4:
    // create an external client that can be configured
    configuredExternalClient =
        jerseyClientBundle
            .getClientFactory()
            .externalClient(configuration.getConfiguredClient())
            .buildGenericClient("configuredExternalClient");
  }

  /**
   * Method only for testing platform client. Should not be implemented in real applications.
   *
   * @return the http status of the response
   */
  @SuppressWarnings("WeakerAccess")
  public int usePlatformClient() {
    return platformClient
        .target(apiABaseUrl)
        .path(CARS_PATH)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get()
        .getStatus();
  }

  /**
   * Method only for testing external client. Should not be implemented in real applications.
   *
   * @return the http status of the response
   */
  @SuppressWarnings("WeakerAccess")
  public int useExternalClient() {
    return externalClient
        .target(apiABaseUrl)
        .path(CARS_PATH)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get()
        .getStatus();
  }

  /**
   * Method only for testing the configured external client. Should not be implemented in real
   * applications.
   *
   * @return the http status of the response
   */
  @SuppressWarnings("WeakerAccess")
  public int useConfiguredExternalClient() {
    return configuredExternalClient
        .target(apiABaseUrl)
        .path(CARS_PATH)
        .request(MediaType.APPLICATION_JSON_TYPE)
        .get()
        .getStatus();
  }

  /**
   * Method only for testing api client. Should not be implemented in real applications.
   *
   * @return list of cars that is the result of the service invocation
   */
  @SuppressWarnings("WeakerAccess")
  public List<Car> useApiClient() {
    return apiClient.getCars();
  }
}
