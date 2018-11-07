package com.sdase.commons.server.consumer;

public class ConsumerTokenConfig {

   private boolean optional;

   /**
    * @return if the consumer token is optional. Provided consumer tokens will still be registered in the request
    *         context but the request will not fail with 401 if no token is provided.
    */
   public boolean isOptional() {
      return optional;
   }

   /**
    * @param optional if the consumer token is optional. Provided consumer tokens will still be registered in the
    *                 request context but the request will not fail with 401 if no token is provided.
    */
   public void setOptional(boolean optional) {
      this.optional = optional;
   }
}
