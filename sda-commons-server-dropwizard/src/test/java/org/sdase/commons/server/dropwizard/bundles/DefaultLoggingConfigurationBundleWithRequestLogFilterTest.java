package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.dropwizard.Configuration;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.filter.UriFilterFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.dropwizard.bundles.test.RequestLoggingTestApp;

@SetSystemProperty(key = "DISABLE_HEALTHCHECK_LOGS", value = "true")
class DefaultLoggingConfigurationBundleWithRequestLogFilterTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          RequestLoggingTestApp.class, resourceFilePath("without-appenders-key-config.yaml"));

  @Test
  void shouldApplyRequestLogFilterFactories() {
    RequestLoggingTestApp app = DW.getApplication();

    DefaultServerFactory serverFactory =
        (DefaultServerFactory) app.getConfiguration().getServerFactory();
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();

    assertThat(requestLogFactory.getAppenders()).isNotEmpty();
    ConsoleAppenderFactory<?> consoleAppenderFactory =
        (ConsoleAppenderFactory<?>) requestLogFactory.getAppenders().get(0);

    assertThat(consoleAppenderFactory.getFilterFactories()).isNotEmpty();
    UriFilterFactory uriFilterFactory =
        (UriFilterFactory) consoleAppenderFactory.getFilterFactories().get(0);

    assertThat(uriFilterFactory).isNotNull();
    assertThat(uriFilterFactory.getUris())
        .containsExactlyInAnyOrder(
            "/ping", "/healthcheck", "/healthcheck/internal", "/metrics", "/metrics/prometheus");
  }

  @StdIo
  @Test
  void shouldNotLogExcludedPaths(StdOut out) {
    List<String> pathsExpectingNoLogEntry =
        Arrays.asList(
            "ping", "healthcheck", "healthcheck/internal", "metrics", "metrics/prometheus");
    List<Response> responses = new ArrayList<>();
    try {
      pathsExpectingNoLogEntry.forEach(p -> createAdminTarget().path(p).request().get());
      responses.add(createWebTarget().path("test").request().get());

      assertThat(responses)
          .extracting(Response::getStatus)
          .allMatch(sc -> sc == OK.getStatusCode());

      await()
          .untilAsserted(() -> assertThat(out.capturedLines()).anyMatch(l -> l.contains("/test")));

      assertThat(String.join("\n", out.capturedLines())).doesNotContain(pathsExpectingNoLogEntry);
    } finally {
      responses.forEach(Response::close);
    }
  }

  @StdIo
  @Test
  void verifyDeprecatedEndpointIsLogged(StdOut out) {
    List<String> deprecatedPaths = Collections.singletonList("healthcheck/prometheus");
    List<Response> responses = new ArrayList<>();
    try {
      // invoke deprecated paths
      deprecatedPaths.forEach(p -> createAdminTarget().path(p).request().get());

      // invoke "test" to definitely get some output
      responses.add(createWebTarget().path("test").request().get());

      assertThat(responses)
          .extracting(Response::getStatus)
          .allMatch(sc -> sc == OK.getStatusCode());

      await()
          .untilAsserted(() -> assertThat(out.capturedLines()).anyMatch(l -> l.contains("/test")));

      assertThat(String.join("\n", out.capturedLines())).contains(deprecatedPaths);
    } finally {
      responses.forEach(Response::close);
    }
  }

  private WebTarget createWebTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getLocalPort()));
  }

  private WebTarget createAdminTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }
}
