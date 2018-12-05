package org.sdase.commons.shared.api.error;

import java.io.Serializable;

/**
 * Invalid parameter information within an {@link ApiError}
 */
public class ApiInvalidParam implements Serializable {

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
