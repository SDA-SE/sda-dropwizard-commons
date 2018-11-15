package org.sdase.commons.server.consumer;

import java.util.ArrayList;
import java.util.List;

public class ConsumerTokenConfig {

   private boolean optional;

   private List<String> excludePatterns = new ArrayList<>();

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

   /**
    * @return returns a list of regex pattern for paths that are excluded from the filter
    */
   public List<String> getExcludePatterns() {
      return excludePatterns;
   }

   /**
    *
    * @param excludePatterns list with pattern that are excluded from the configuration filter
    */
   public void setExcludePatterns(List<String> excludePatterns) {
      this.excludePatterns = excludePatterns;
   }
}
