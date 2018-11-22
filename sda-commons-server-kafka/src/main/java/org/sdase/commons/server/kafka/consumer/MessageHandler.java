package org.sdase.commons.server.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Handler for consumer record processing. The handler includes the business logic that should be applied on
 * consumed messages
 * @param <K> key class
 * @param <V> value class
 */
@FunctionalInterface
public interface MessageHandler<K, V> {
   void handle(ConsumerRecord<K, V> record);
}
