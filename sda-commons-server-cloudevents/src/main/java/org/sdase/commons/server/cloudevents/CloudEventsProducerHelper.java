package org.sdase.commons.server.cloudevents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

public class CloudEventsProducerHelper<T> {

  private final ObjectMapper objectMapper;

  private final URI source;

  private final String type;

  /**
   * @param objectMapper the object mapper
   * @param source the source of new CloudEvents
   * @param type the type of new CloudEvents
   */
  public CloudEventsProducerHelper(ObjectMapper objectMapper, URI source, String type) {
    this.objectMapper = objectMapper;
    this.source = source;
    this.type = type;
  }

  /**
   * Returns a new {@link CloudEvent} that contains the given {@code event} as data. The event will
   * be serialized as JSON using Jackson's {@link ObjectMapper}.
   *
   * @param event the event
   * @param subject the subject of the CloudEvent
   * @return a new {@link CloudEvent}
   * @throws JsonProcessingException if the given object could not be serialized
   * @see <a href="https://github.com/cloudevents/spec/blob/v1.0.2/cloudevents/spec.md">CloudEvent
   *     specification</a>
   */
  public CloudEvent wrap(T event, String subject) throws JsonProcessingException {
    byte[] data = objectMapper.writeValueAsBytes(event);

    return CloudEventBuilder.v1()
        .withSource(source)
        .withId(UUID.randomUUID().toString())
        .withType(type)
        .withSubject(subject)
        .withTime(OffsetDateTime.now())
        .withDataContentType("application/json")
        .withData(data)
        .build();
  }
}
