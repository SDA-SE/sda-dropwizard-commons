package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.logging.ConsoleAppenderFactory;
import io.dropwizard.request.logging.LogbackAccessRequestLogFactory;
import io.dropwizard.request.logging.filter.UriFilterFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
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
  @StdIo
  void shouldApplyRequestLogFilterFactories(StdOut out) {
    RequestLoggingTestApp app = DW.getApplication();

    DefaultServerFactory serverFactory =
        (DefaultServerFactory) app.getConfiguration().getServerFactory();
    LogbackAccessRequestLogFactory requestLogFactory =
        (LogbackAccessRequestLogFactory) serverFactory.getRequestLogFactory();

    assertThat(requestLogFactory.getAppenders()).isNotEmpty();
    ConsoleAppenderFactory consoleAppenderFactory =
        (ConsoleAppenderFactory) requestLogFactory.getAppenders().get(0);

    assertThat(consoleAppenderFactory.getFilterFactories()).isNotEmpty();
    UriFilterFactory uriFilterFactory =
        (UriFilterFactory) consoleAppenderFactory.getFilterFactories().get(0);

    Response response =
        createAdminTarget().path("healthcheck/internal").request().buildGet().invoke();

    assertThat(uriFilterFactory).isNotNull();
    assertThat(uriFilterFactory.getUris()).containsExactly("/ping", "/healthcheck/internal");

    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    assertThat(out.capturedLines()).noneMatch(line -> line.contains("/healthcheck/internal"));
  }

  private WebTarget createAdminTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getAdminPort()));
  }
}
