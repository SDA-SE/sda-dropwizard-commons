package org.sdase.commons.shared.api.error;

import java.util.ArrayList;
import java.util.List;


/**
 * Exception that should be thrown within rest services.
 */
public class ApiException extends RuntimeException {

   private final int httpCode;
   private final String title;
   private final List<ApiInvalidParam> invalidParams; // NOSONAR

   private ApiException(int httpCode, String title, List<ApiInvalidParam> invalidParams, Throwable cause) {
      super(title, cause);
      this.httpCode = httpCode;
      this.title = title;
      this.invalidParams = invalidParams;
   }

   public int getHttpCode() {
      return httpCode;
   }

   public ApiError getDTO() {
      return new ApiError(title, invalidParams);
   }

   public static HttpCodeBuilder builder() {
      return new Builder();
   }

   public interface FinalBuilder {
      FinalBuilder detail(String field, String reason, String errorCode);
      FinalBuilder cause(Throwable cause);
      ApiException build();
   }

   public interface HttpCodeBuilder {
      TitleBuilder httpCode(int code);
   }

   public interface TitleBuilder {
      FinalBuilder title(String title);
   }

   public static class Builder implements TitleBuilder, HttpCodeBuilder, FinalBuilder{
      private int httpCode;
      private String title;
      private List<ApiInvalidParam> apiInvalidParams = new ArrayList<>();
      private Throwable cause = null;


      @Override
      public FinalBuilder detail(String field, String reason, String errorCode) {
         apiInvalidParams.add(new ApiInvalidParam(field, reason, errorCode));
         return this;
      }

      @Override
      public FinalBuilder cause(Throwable cause) {
         this.cause = cause;
         return this;
      }

      @Override
      public ApiException build() {
         return new ApiException(httpCode, title, apiInvalidParams, cause);
      }

      @Override
      public TitleBuilder httpCode(int code) {
         if (code < 400 || code > 599) {
            throw new IllegalStateException("Error code must be of range (400, 599)");
         }
         this.httpCode = code;
         return this;
      }

      @Override
      public FinalBuilder title(String title) {
         this.title = title;
         return this;
      }
   }


}
