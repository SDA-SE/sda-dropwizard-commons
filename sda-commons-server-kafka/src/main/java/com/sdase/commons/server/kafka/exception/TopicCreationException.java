package com.sdase.commons.server.kafka.exception;

public class TopicCreationException extends RuntimeException {

   public TopicCreationException(String messsage, Throwable c) {
      super(messsage, c);
   }
}
