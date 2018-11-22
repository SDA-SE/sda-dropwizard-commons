package org.sdase.commons.server.kafka.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Simple error handler that ignores errors and returns true so that the
 * MessageListener will not stop
 * @param <K> key of record
 * @param <V> value of record
 */
public class IgnoreAndProceedErrorHandler<K, V> implements ErrorHandler<K, V> {

   @Override
   public boolean handleError(ConsumerRecord<K, V> record, RuntimeException e, Consumer<K, V> consumer) {
      // do nothing. Just ignore the error
      return true;
   }

}
