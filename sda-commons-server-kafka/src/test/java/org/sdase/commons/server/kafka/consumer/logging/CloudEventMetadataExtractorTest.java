package org.sdase.commons.server.kafka.consumer.logging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CloudEventMetadataExtractorTest {

  @Test
  void shouldExtractAttributesFromStructuredCloudEvent() {
    // given
    var extractor = CloudEventMetadataExtractor.fromJson(new ObjectMapper());
    var event =
        new TestEvent(
            "event-1", URI.create("/source"), "example.created", Map.of("secret", "value"));

    // when
    var metadata = extractor.extract(event);

    // then
    assertThat(metadata).isEqualTo(new CloudEventMetadata("event-1", "/source", "example.created"));
  }

  @Test
  void shouldLeaveMissingAttributesEmpty() {
    // given
    var extractor = CloudEventMetadataExtractor.fromJson(new ObjectMapper());

    // when
    var metadata = extractor.extract(Map.of("data", "value"));

    // then
    assertThat(metadata).isEqualTo(new CloudEventMetadata(null, null, null));
  }

  private record TestEvent(String id, URI source, String type, Map<String, String> data) {}
}
