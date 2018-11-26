package org.sdase.commons.server.kafka.consumer;

/**
 * Exception to stop message listening in case of errors during message processing.
 * The exception is used internally in the message handler to break the polling loop.
 *
 * It should not be thrown elsewhere. You should use return type of your
 * {@link org.sdase.commons.server.kafka.consumer.ErrorHandler} to stop listening.
 */
class StopListenerException extends RuntimeException {

   protected StopListenerException(Throwable cause) {
      super(cause);
   }

}
