package org.sdase.commons.server.jackson.errors;

import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

/**
 * Exception that should be thrown within rest services.
 */
public class ApiException extends RuntimeException {

   private final int httpCode;
   private final String title;
   private final List<ApiInvalidParam> invalidParams;

   private ApiException(int httpCode, String title, List<ApiInvalidParam> invalidParams) {
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


      @Override
      public FinalBuilder detail(String field, String reason, String errorCode) {
         apiInvalidParams.add(new ApiInvalidParam(field, reason, errorCode));
         return this;
      }

      @Override
      public ApiException build() {
         return new ApiException(httpCode, title, apiInvalidParams);
      }

      @Override
      public TitleBuilder httpCode(int code) {
         this.httpCode = code;
         if (Response.Status.Family.SERVER_ERROR != Response.Status.Family.familyOf(code) &&
               Response.Status.Family.CLIENT_ERROR != Response.Status.Family.familyOf(code)) {
            throw new IllegalStateException("Error code must be of range (400, 599)");
         }
         return this;
      }

      @Override
      public FinalBuilder title(String title) {
         this.title = title;
         return this;
      }
   }


}
