package org.sdase.commons.starter;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.starter.test.StarterApp;

public class SdaPlatformBundleAppTest {

  @ClassRule
  public static final DropwizardAppRule<SdaPlatformConfiguration> DW =
      new DropwizardAppRule<>(
          StarterApp.class,
          resourceFilePath("test-config.yaml"),
          config("auth.disableAuth", "true"));

  @Test
  public void pongForPing() {
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
