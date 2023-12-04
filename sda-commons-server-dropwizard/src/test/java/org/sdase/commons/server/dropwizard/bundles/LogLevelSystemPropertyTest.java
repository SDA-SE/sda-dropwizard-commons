package org.sdase.commons.server.dropwizard.bundles;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.core.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;
import org.sdase.commons.server.dropwizard.bundles.test.RequestLoggingTestApp;

@SetSystemProperty(key = "dw.logging.level", value = "DEBUG")
class LogLevelSystemPropertyTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          RequestLoggingTestApp.class, resourceFilePath("with-empty-config.yaml"));

  @StdIo
  @Test
  void shouldProduceDebugOutput(StdOut out) {

    try (Response ignored = createWebTarget().path("test").request().get()) {

      assertThat(out.capturedLines()).anyMatch(l -> l.contains("[DEBUG]"));
    }
  }

  private WebTarget createWebTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getLocalPort()));
  }
}
