package org.sdase.commons.server.opa.filter.model;


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

      private String constraints;

      public boolean isAllow() {
         return allow;
      }

      public Content setAllow(boolean allow) {
         this.allow = allow;
         return this;
      }

      public String getConstraints() {
         return constraints;
      }

      public Content setConstraints(
          String constraints) {
         this.constraints = constraints;
         return this;
      }
   }
}