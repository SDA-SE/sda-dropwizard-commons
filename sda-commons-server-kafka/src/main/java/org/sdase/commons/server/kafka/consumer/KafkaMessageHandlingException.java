package org.sdase.commons.server.kafka.consumer;

public class KafkaMessageHandlingException extends RuntimeException {

   /**
    * 
    */
   private static final long serialVersionUID = 266166785353522569L;

   public KafkaMessageHandlingException(String message) {
      super(message);
   }

   KafkaMessageHandlingException() {
      super();
   }

   public KafkaMessageHandlingException(String message, Throwable cause) {
      super(message, cause);
   }

   public KafkaMessageHandlingException(Throwable cause) {
      super(cause);
   }

}
