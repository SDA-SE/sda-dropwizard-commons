package org.sdase.commons.server.cloudevents;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.CloudEventData;
import java.io.IOException;

public class CloudEventsConsumerHelper {

  private final ObjectMapper objectMapper;

  public CloudEventsConsumerHelper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Will return the payload of the {@link CloudEvent} that can be found in the `data` field. We
   * expect it to be serialized as `application/json` and will use Jackson's {@link ObjectMapper} to
   * deserialize it.
   *
   * @param cloudEvent the CloudEvent
   * @param type the target type
   * @param <T> the target type
   * @return the payload of the CloudEvent as the desired type or {@code null} if `data` was empty
   * @throws IllegalStateException if the content-type is something else than `application/json`
   * @throws IOException if `data` could not be deserialized
   */
  public <T> T unwrap(CloudEvent cloudEvent, Class<T> type) throws IOException {
    String dataContentType = cloudEvent.getDataContentType();
    if (dataContentType == null || !dataContentType.equalsIgnoreCase("application/json")) {
      throw new IllegalStateException("Unsupported content-type: " + dataContentType);
    }

    CloudEventData data = cloudEvent.getData();
    if (data == null) {
      return null;
    }

    byte[] bytes = data.toBytes();
    if (bytes == null) {
      return null;
    }

    return objectMapper.readValue(bytes, type);
  }
}
