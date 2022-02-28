package org.sdase.commons.server.cloudevents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.cloudevents.app.produce.PartnerCreatedEvent;

class CloudEventsConsumerHelperTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private final CloudEventsConsumerHelper helper = new CloudEventsConsumerHelper(objectMapper);

  @Test
  void shouldThrowIllegalStateExceptionIfContentTypeIsNotJson() {
    // given
    CloudEvent event =
        CloudEventBuilder.v1()
            .withId("4711")
            .withSource(URI.create("/SDA-SE/test"))
            .withType("PARTNER_CREATED")
            .withDataContentType("text/plain")
            .build();

    // when/then
    assertThatCode(() -> helper.unwrap(event, PartnerCreatedEvent.class))
        .isInstanceOf(IllegalStateException.class);
  }

  @Test
  void shouldReturnNullIfNoDataIsPresent() throws IOException {
    // given
    CloudEvent event =
        CloudEventBuilder.v1()
            .withId("4711")
            .withSource(URI.create("/SDA-SE/test"))
            .withType("PARTNER_CREATED")
            .withDataContentType("application/json")
            .build();

    // when
    PartnerCreatedEvent result = helper.unwrap(event, PartnerCreatedEvent.class);

    // then
    assertThat(result).isNull();
  }

  @Test
  void shouldReturnPartnerCreatedEvent() throws IOException {
    // given
    PartnerCreatedEvent originalEvent = new PartnerCreatedEvent().setId("partnerId");
    CloudEvent event =
        CloudEventBuilder.v1()
            .withId("4711")
            .withSource(URI.create("/SDA-SE/test"))
            .withType("PARTNER_CREATED")
            .withDataContentType("application/json")
            .withData(objectMapper.writeValueAsBytes(originalEvent))
            .build();

    // when
    PartnerCreatedEvent result = helper.unwrap(event, PartnerCreatedEvent.class);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo("partnerId");
  }
}
