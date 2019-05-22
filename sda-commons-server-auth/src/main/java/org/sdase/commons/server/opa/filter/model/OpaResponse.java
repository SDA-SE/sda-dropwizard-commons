package org.sdase.commons.server.opa.filter.model;

import com.fasterxml.jackson.databind.JsonNode;

public class OpaResponse {

   private Content result;

   public Content getResult() {
      return result;
   }

   public OpaResponse setResult(Content result) {
      this.result = result;
      return this;
   }

   public static class Content {

      private boolean allow;

      private JsonNode constraints;

      public boolean isAllow() {
         return allow;
      }

      public Content setAllow(boolean allow) {
         this.allow = allow;
         return this;
      }

      public JsonNode getConstraints() {
         return constraints;
      }

      public Content setConstraints(JsonNode constraints) {
         this.constraints = constraints;
         return this;
      }
   }
}
