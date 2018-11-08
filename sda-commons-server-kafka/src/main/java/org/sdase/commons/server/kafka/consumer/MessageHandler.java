package org.sdase.commons.server.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Marks implementing classes as handlers for incoming messages of type V
 * 
 * @author Tim Braun <tim.braun@de.ibm.com>
 *
 * @param <V>
 *           Generic type of Message
 */
@FunctionalInterface
public interface MessageHandler<K, V> {
   void handle(ConsumerRecord<K, V> record);
}
