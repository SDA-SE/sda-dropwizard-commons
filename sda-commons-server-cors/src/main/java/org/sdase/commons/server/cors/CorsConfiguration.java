package org.sdase.commons.server.cors;

import java.util.ArrayList;
import java.util.List;

public class CorsConfiguration {

   private List<String> allowedOrigins = new ArrayList<>();

   public List<String> getAllowedOrigins() {
      return allowedOrigins;
   }

   public void setAllowedOrigins(List<String> allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
   }
}
