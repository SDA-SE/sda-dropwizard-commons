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
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.dropwizard.bundles.test.RequestLoggingTestApp;

@SetSystemProperty(key = "DISABLE_HEALTHCHECK_LOGS", value = "true")
class DefaultLoggingConfigurationBundleWithRequestLogFilterTest {

  @Test
  @StdIo
  void shouldApplyRequestLogFilterFactories(StdOut out) throws Exception {

    // FIXME junit-pioneer changed that @SetSystemProperty is applied as beforeEach since 1.7.0
    // So the property is not available when the app extension is used as class extension.
    // See https://github.com/junit-pioneer/junit-pioneer/issues/623
    DropwizardAppExtension<Configuration> dw =
        new DropwizardAppExtension<>(
            RequestLoggingTestApp.class, resourceFilePath("without-appenders-key-config.yaml"));
    try {
      dw.before();

      RequestLoggingTestApp app = dw.getApplication();

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

      Response response =
          createAdminTarget(dw).path("healthcheck/internal").request().buildGet().invoke();

      assertThat(uriFilterFactory).isNotNull();
      assertThat(uriFilterFactory.getUris()).containsExactly("/ping", "/healthcheck/internal");

      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      assertThat(out.capturedLines()).noneMatch(line -> line.contains("/healthcheck/internal"));
    } finally {
      dw.after();
    }
  }

  private WebTarget createAdminTarget(DropwizardAppExtension<?> dw) {
    return dw.client().target(String.format("http://localhost:%d/", dw.getAdminPort()));
  }
}
