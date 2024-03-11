package org.sdase.commons.server.kafka.producer;

import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.sdase.commons.shared.tracing.TraceTokenContext;

/**
 * A delegating {@link Producer} that adds an additional {@value
 * TraceTokenContext#TRACE_TOKEN_MESSAGING_HEADER_NAME} header if the message is sent in a {@link
 * TraceTokenContext}.
 *
 * @param <K> the type of the message key
 * @param <V> the type of the message value
 */
public class TraceTokenAwareKafkaProducer<K, V>
    extends AbstractDelegatingAdditionalHeadersProducer<K, V> {

  public TraceTokenAwareKafkaProducer(Producer<K, V> delegate) {
    super(delegate);
  }

  @Override
  protected Headers additionalHeaders(ProducerRecord<K, V> producerRecord) {
    if (!TraceTokenContext.isTraceTokenContextActive()) {
      return null;
    }

    try (var traceTokenContext = TraceTokenContext.getOrCreateTraceTokenContext()) {
      var headers = new RecordHeaders(producerRecord.headers());
      headers.add(
          TraceTokenContext.TRACE_TOKEN_MESSAGING_HEADER_NAME,
          traceTokenContext.get().getBytes(StandardCharsets.UTF_8));

      return headers;
    }
  }
}
