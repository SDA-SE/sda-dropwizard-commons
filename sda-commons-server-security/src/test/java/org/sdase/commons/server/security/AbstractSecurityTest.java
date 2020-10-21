package org.sdase.commons.server.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.AbstractServerFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.server.ServerFactory;
import io.dropwizard.server.SimpleServerFactory;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test that checks the application with a given config for vulnerabilities. This test may be Used
 * as a template to check other applications for security risks. Usage:
 *
 * <pre>
 *   public class MyAppIsSecureIT extends AbstractSecurityTest<Configuration> {
 *
 *     &#64;ClassRule
 *     public static final DropwizardAppRule<Configuration> DW = new DropwizardAppRule<>(
 *         MyApp.class,
 *         ResourceHelpers.resourceFilePath("default-config.yaml")
 *     );
 *
 *     &#64;Override
 *     DropwizardAppRule<Configuration> getAppRule() {
 *       return DW;
 *     }
 *
 *     // add custom security checks here if needed
 *   }
 * }</pre>
 */
public abstract class AbstractSecurityTest<C extends Configuration> {

  abstract DropwizardAppRule<C> getAppRule();

  private WebTarget appClient;
  private WebTarget adminClient;

  @Before
  public void setUp() {
    appClient =
        getAppRule()
            .client()
            .target(String.format("http://localhost:%s", getAppRule().getLocalPort()));
    adminClient =
        getAppRule()
            .client()
            .target(String.format("http://localhost:%s", getAppRule().getAdminPort()));
  }

  @Test
  public void doNotAllowTrace() {
    Set<String> allowedMethods =
        getServerFactory().getAllowedMethods().stream()
            .map(String::trim)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    assertThat(allowedMethods).isNotEmpty().doesNotContain("trace");
  }

  @Test
  public void doNotStartAsRoot() {
    assertThat(getServerFactory().getStartsAsRoot()).isFalse();
  }

  @Test
  public void useForwardHeadersInApp() {
    assertThat(getAppConnector().isUseForwardedHeaders()).isTrue();
  }

  @Test
  public void useForwardHeadersInAdmin() {
    assertThat(getAdminConnector().isUseForwardedHeaders()).isTrue();
  }

  @Test
  public void doNotUseServerHeaderInApp() {
    assertThat(getAppConnector().isUseServerHeader()).isFalse();
    Response response =
        getAppClient()
            .path("path")
            .path("does")
            .path("not")
            .path("exist")
            .request()
            .get(); // NOSONAR
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getHeaderString("Server")).isBlank();
  }

  @Test
  public void doNotUseServerHeaderInAdmin() {
    assertThat(getAdminConnector().isUseServerHeader()).isFalse();
    Response response =
        getAdminClient().path("path").path("does").path("not").path("exist").request().get();
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getHeaderString("Server")).isBlank();
  }

  @Test
  public void doNotUseDateHeaderInApp() {
    assertThat(getAppConnector().isUseDateHeader()).isFalse();
    Response response =
        getAppClient().path("path").path("does").path("not").path("exist").request().get();
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getHeaderString("Date")).isBlank();
  }

  @Test
  public void doNotUseDateHeaderInAdmin() {
    assertThat(getAdminConnector().isUseDateHeader()).isFalse();
    Response response =
        getAppClient().path("path").path("does").path("not").path("exist").request().get();
    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(response.getHeaderString("Date")).isBlank();
  }

  @Test
  public void doNotShowDefaultErrorPageInApp() {
    Response response =
        getAppClient()
            .path("path")
            .path("does")
            .path("not")
            .path("exist")
            .request(MediaType.APPLICATION_JSON)
            .get();
    assertThat(response.getStatus()).isEqualTo(404);
    String content = response.readEntity(String.class);
    // should not render the default error object of jetty which writes a json with a property code
    // that contains only
    // the http status
    assertThat(content).doesNotMatch(".*\"code\"\\s*:\\s*404.*");
  }

  // represents the use of the BufferLimitsAdvice as the input headers are the only thing that can
  // be checked from http
  @Test
  public void rejectInputHeadersOverEightKib() {
    String chars = "0987654321abcdefghijklmnopqrstuvwxyz";
    StringBuilder valueMoreThanOneKib = new StringBuilder();
    while (valueMoreThanOneKib.length() < 1024) {
      valueMoreThanOneKib.append(chars);
    }
    Response response =
        getAppClient()
            .request(MediaType.APPLICATION_JSON)
            .header("X-Header-One", valueMoreThanOneKib.toString())
            .header("X-Header-Two", valueMoreThanOneKib.toString())
            .header("X-Header-Three", valueMoreThanOneKib.toString())
            .header("X-Header-Four", valueMoreThanOneKib.toString())
            .header("X-Header-Five", valueMoreThanOneKib.toString())
            .header("X-Header-Six", valueMoreThanOneKib.toString())
            .header("X-Header-Seven", valueMoreThanOneKib.toString())
            .header("X-Header-Eight", valueMoreThanOneKib.toString())
            .get();
    assertThat(response.getStatus()).isEqualTo(431); // Request Header Fields Too Large
    response.close();
  }

  @Test
  @Ignore(
      "Setting a custom error handler does not affect the deep internals of Jetty.\n"
          + "There has been an issue https://github.com/dropwizard/dropwizard/issues/647 but the resolution only affects"
          + "regular error responses from the application.\n"
          + "In AbstractServerFactory#568 a default ErrorHandler is registered.")
  public void rejectInputHeadersOverEightKibNotReturningDefaultErrorPage() {
    String chars = "0987654321abcdefghijklmnopqrstuvwxyz";
    StringBuilder valueMoreThanOneKib = new StringBuilder();
    while (valueMoreThanOneKib.length() < 1024) {
      valueMoreThanOneKib.append(chars);
    }
    Response response =
        getAppClient()
            .request(MediaType.APPLICATION_JSON)
            .header("X-Header-One", valueMoreThanOneKib.toString())
            .header("X-Header-Two", valueMoreThanOneKib.toString())
            .header("X-Header-Three", valueMoreThanOneKib.toString())
            .header("X-Header-Four", valueMoreThanOneKib.toString())
            .header("X-Header-Five", valueMoreThanOneKib.toString())
            .header("X-Header-Six", valueMoreThanOneKib.toString())
            .header("X-Header-Seven", valueMoreThanOneKib.toString())
            .header("X-Header-Eight", valueMoreThanOneKib.toString())
            .get();
    String responseBodyRaw = response.readEntity(String.class);
    assertThat(responseBodyRaw).doesNotMatch(".*<[^>]+>.*"); // no HTML
  }

  /**
   * @return the only {@link HttpConnectorFactory} for the application port
   * @throws AssertionError if not exactly one {@link HttpConnectorFactory} is configured for the
   *     application port
   */
  @SuppressWarnings("WeakerAccess")
  protected HttpConnectorFactory getAppConnector() {
    ServerFactory serverFactory = getAppRule().getConfiguration().getServerFactory();
    assertThat(serverFactory)
        .isInstanceOfAny(DefaultServerFactory.class, SimpleServerFactory.class);
    if (serverFactory instanceof DefaultServerFactory) {
      DefaultServerFactory defaultServerFactory = (DefaultServerFactory) serverFactory;
      List<ConnectorFactory> applicationConnectors =
          defaultServerFactory.getApplicationConnectors();
      assertThat(applicationConnectors).hasSize(1);
      ConnectorFactory connectorFactory = applicationConnectors.get(0);
      assertThat(connectorFactory).isInstanceOf(HttpConnectorFactory.class);
      return (HttpConnectorFactory) connectorFactory;
    } else if (serverFactory instanceof SimpleServerFactory) {
      SimpleServerFactory simpleServerFactory = (SimpleServerFactory) serverFactory;
      ConnectorFactory connectorFactory = simpleServerFactory.getConnector();
      assertThat(connectorFactory).isInstanceOf(HttpConnectorFactory.class);
      return (HttpConnectorFactory) connectorFactory;
    } else {
      return null; // should not reach this
    }
  }

  /**
   * @return the only {@link HttpConnectorFactory} for the admin port
   * @throws AssertionError if not exactly one {@link HttpConnectorFactory} is configured for the
   *     application port
   */
  @SuppressWarnings("WeakerAccess")
  protected HttpConnectorFactory getAdminConnector() {
    ServerFactory serverFactory = getAppRule().getConfiguration().getServerFactory();
    assertThat(serverFactory).isInstanceOf(DefaultServerFactory.class);
    if (serverFactory instanceof DefaultServerFactory) {
      DefaultServerFactory defaultServerFactory = (DefaultServerFactory) serverFactory;
      List<ConnectorFactory> applicationConnectors = defaultServerFactory.getAdminConnectors();
      assertThat(applicationConnectors).hasSize(1);
      ConnectorFactory connectorFactory = applicationConnectors.get(0);
      assertThat(connectorFactory).isInstanceOf(HttpConnectorFactory.class);
      return (HttpConnectorFactory) connectorFactory;
    } else {
      return null; // should not reach this
    }
  }

  /**
   * @return the {@link AbstractServerFactory} that is used to configure the application
   * @throws AssertionError if the {@link Configuration#getServerFactory()} is not an {@link
   *     AbstractServerFactory}
   */
  @SuppressWarnings("WeakerAccess")
  protected AbstractServerFactory getServerFactory() {
    ServerFactory serverFactory = getAppRule().getConfiguration().getServerFactory();
    assertThat(serverFactory).isInstanceOf(AbstractServerFactory.class);
    return (AbstractServerFactory) serverFactory;
  }

  /** @return a {@link WebTarget} pointing to the root of the application port */
  @SuppressWarnings("WeakerAccess")
  protected WebTarget getAppClient() {
    return this.appClient;
  }

  /** @return a {@link WebTarget} pointing to the root of the admin port */
  @SuppressWarnings("WeakerAccess")
  protected WebTarget getAdminClient() {
    return this.adminClient;
  }
}
