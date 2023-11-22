package org.sdase.commons.client.jersey.test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.sdase.commons.client.jersey.ClientFactory;
import org.sdase.commons.client.jersey.oidc.filter.OidcRequestFilter;
import org.sdase.commons.client.jersey.test.MockApiClient.Car;

@Path("/api")
public class ClientTestEndPoint {
  private MockApiClient mockApiClient;
  private MockApiClient externalMockApiClient;
  private MockApiClient authMockApiClient;

  ClientTestEndPoint(
      ClientFactory clientFactory, String baseUrl, OidcRequestFilter oidcRequestFilter) {
    mockApiClient =
        clientFactory
            .platformClient()
            .addFilter(oidcRequestFilter)
            .enableAuthenticationPassThrough()
            .api(MockApiClient.class, "MockApiClientWithoutAuth")
            .atTarget(baseUrl);

    setupClients(clientFactory, baseUrl);
  }

  ClientTestEndPoint(ClientFactory clientFactory, String baseUrl) {
    mockApiClient =
        clientFactory
            .platformClient()
            .api(MockApiClient.class, "MockApiClientWithoutAuth")
            .atTarget(baseUrl);

    setupClients(clientFactory, baseUrl);
  }

  private void setupClients(ClientFactory clientFactory, String baseUrl) {
    authMockApiClient =
        clientFactory
            .platformClient()
            .enableAuthenticationPassThrough()
            .api(MockApiClient.class, "MockApiClientWithAuth")
            .atTarget(baseUrl);
    externalMockApiClient =
        clientFactory
            .externalClient()
            .api(MockApiClient.class, "MockApiClientExternal")
            .atTarget(baseUrl);
  }

  @GET
  @Path("/cars")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delegate() {
    return mockApiClient.requestCars();
  }

  @GET
  @Path("/cars/{sign}")
  @Produces(MediaType.APPLICATION_JSON)
  public Car delegateGetCar(@PathParam("sign") String sign) {
    return mockApiClient.getCar(sign);
  }

  @GET
  @Path("/carsExternal")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delegateExternal() {
    return externalMockApiClient.requestCars();
  }

  @GET
  @Path("/carsAuth")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delegateWithAuth() {
    return authMockApiClient.requestCars();
  }
}
