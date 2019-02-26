package org.sdase.commons.server.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Interface for handling errors that might occur during the processing of Kafka records.
 * Be aware that the key or value can be null if the WrappedNoSerializationErrorDeserializer
 * is used.
 * @param <K> key class of the record
 * @param <V> value class of the record
 */
@FunctionalInterface
public interface ErrorHandler<K, V> {

   /**
    * handling error that happenes during processing of the consumer record
    * @param record consumer record that caused the error
    * @param e error that occured
    * @param consumer consumer that read the record
    * @return if false, the listener should stop consuming messages
    */
   boolean handleError(ConsumerRecord<K, V> record, RuntimeException e, Consumer<K, V> consumer);
}
