package org.sdase.commons.server.kafka.consumer.strategies.legacy;

import java.util.Map;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.sdase.commons.server.kafka.consumer.MessageHandler;

/**
 * Interface extension of the simple {@link MessageHandler} to allow the handling of commit
 * callbacks
 *
 * @param <K> the key object type
 * @param <V> the value object type
 */
public interface CallbackMessageHandler<K, V> extends MessageHandler<K, V> {

  void handleCommitCallback(Map<TopicPartition, OffsetAndMetadata> offsets, Exception exception);
}
