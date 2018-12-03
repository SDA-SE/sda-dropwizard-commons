package org.sdase.commons.shared.api.error;


import java.util.ArrayList;
import java.util.List;

/**
 * Error Entity object for transferring error information between server and client.
 */
public class ApiError {

   private String title;

   private List<ApiInvalidParam> invalidParams;

   public ApiError() {
      // default constructor
   }

   public ApiError(String title) {
      this.title = title;
      this.invalidParams = new ArrayList<>();
   }
   
   public ApiError(String title, List<ApiInvalidParam> invalidParams) {
      this.title = title;
      this.invalidParams = invalidParams;
   }

   public String getTitle() {
      return title;
   }

   public void setTitle(String title) {
      this.title = title;
   }

   public List<ApiInvalidParam> getInvalidParams() {
      return invalidParams;
   }

   public void addInvalidParameter(ApiInvalidParam parameter) {
      invalidParams.add(parameter);
   }

}
