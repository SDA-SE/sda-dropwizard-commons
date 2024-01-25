package org.sdase.commons.server.openapi;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import io.dropwizard.core.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.client.Invocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.openapi.apps.test.OpenApiBundleTestApp;

class Iso8601SerializerOpenAPIDocumentationTest {

  @RegisterExtension
  static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          OpenApiBundleTestApp.class, resourceFilePath("test-config.yaml"));

  @Test
  void shouldProduceDateTimeTypeHint() {
    JsonNode response = getJsonRequest().get(JsonNode.class);
    JsonNode partnerSearchResultResource =
        response.get("components").get("schemas").get("PartnerSearchResultResource");
    var timestamp = partnerSearchResultResource.get("properties").get("timestamp");
    assertThat(timestamp.get("format").asText()).isEqualTo("date-time");
  }

  private static Invocation.Builder getJsonRequest() {
    return DW.client()
        .target("http://localhost:" + DW.getLocalPort())
        .path("/api")
        .path("/openapi.json")
        .request(APPLICATION_JSON);
  }
}
