package org.sdase.commons.server.cors;

import java.util.ArrayList;
import java.util.List;

public class CorsConfiguration {

   private List<String> allowedOrigins = new ArrayList<>();

   private List<String> allowedHeaders = new ArrayList<>();

   public List<String> getAllowedOrigins() {
      if (allowedOrigins == null) {
         return new ArrayList<>();
      }
      return allowedOrigins;
   }

   public void setAllowedOrigins(List<String> allowedOrigins) {
      this.allowedOrigins = allowedOrigins;
   }

   public List<String> getAllowedHeaders() {
      if (allowedHeaders == null) {
         return new ArrayList<>();
      }
      return allowedHeaders;
   }

   public void setAllowedHeaders(List<String> allowedHeaders) {
      this.allowedHeaders = allowedHeaders;
   }
}
