package org.sdase.commons.starter;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junitpioneer.jupiter.SetSystemProperty;
import org.sdase.commons.server.dropwizard.metadata.DetachedMetadataContext;
import org.sdase.commons.starter.test.StarterApp;

@SetSystemProperty(key = "METADATA_FIELDS", value = "tenant-id")
class MetadataContextIntegrationTest {

  @RegisterExtension
  public static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          StarterApp.class, resourceFilePath("test-config.yaml"), config("opa.disableOpa", "true"));

  @Test
  void shouldTrackContext() {
    var actual =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("metadataContext")
            .request(APPLICATION_JSON)
            .header("tenant-id", "t1")
            .get(DetachedMetadataContext.class);

    assertThat(actual).containsExactly(entry("tenant-id", List.of("t1")));
  }

  @Test
  void shouldTrackContextCaseInsensitive() {
    var actual =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("metadataContext")
            .request(APPLICATION_JSON)
            .header("Tenant-Id", "t1")
            .get(DetachedMetadataContext.class);

    assertThat(actual).containsExactly(entry("tenant-id", List.of("t1")));
  }

  @Test
  void shouldTrackContextWithMultipleValues() {
    var actual =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("api")
            .path("metadataContext")
            .request(APPLICATION_JSON)
            .header("tenant-id", "t1")
            .header("tenant-id", "t2")
            .get(DetachedMetadataContext.class);

    assertThat(actual).containsExactly(entry("tenant-id", List.of("t1", "t2")));
  }
}
