package org.sdase.commons.shared.api.error;

/**
 * Invalid parameter information within an {@link ApiError}
 */
public class ApiInvalidParam {

   private String field;
   private String reason;
   private String errorCode;

   public ApiInvalidParam() {
      // public constructor
   }

   public ApiInvalidParam(String field, String reason, String errorCode) {
      this.field = field;
      this.reason = reason;
      this.errorCode = errorCode;
   }

   public String getField() {
      return field;
   }

   public String getReason() {
      return reason;
   }

   public String getErrorCode() {
      return errorCode;
   }
}
