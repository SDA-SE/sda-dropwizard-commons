package org.sdase.commons.starter;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.starter.test.StarterApp;

class SdaPlatformBundleAppTest {

  @RegisterExtension
  public static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          StarterApp.class, resourceFilePath("test-config.yaml"), config("opa.disableOpa", "true"));

  @Test
  void pongForPing() {
    Map<String, String> actual =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("ping")
            .request(APPLICATION_JSON)
            .header("Consumer-token", "test-consumer")
            .get(new GenericType<Map<String, String>>() {});

    assertThat(actual).containsExactly(entry("ping", "pong"));
  }
}
