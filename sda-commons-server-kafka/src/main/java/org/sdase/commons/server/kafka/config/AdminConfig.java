package org.sdase.commons.server.kafka.config;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AdminConfig {
	
   @JsonProperty(value = "adminClientrequestTimeoutMs")
   private int adminClientrequestTimeoutMs = 5000;
	   
   @JsonProperty(value = "adminRestApi")
   private List<String> adminRestApi = new ArrayList<>();
   
   @JsonProperty(value = "adminSecurity")
   private Security adminSecurity = new Security();

   public int getAdminClientrequestTimeoutMs() {
      return adminClientrequestTimeoutMs;
   }

   public void setAdminClientrequestTimeoutMs(int adminClientrequestTimeoutMs) {
      this.adminClientrequestTimeoutMs = adminClientrequestTimeoutMs;
   }

   public List<String> getAdminRestApi() {
      return adminRestApi;
   }

   public void setAdminRestApi(List<String> adminRestApi) {
      this.adminRestApi = adminRestApi;
   }

   public Security getAdminSecurity() {
      return adminSecurity;
   }

   public void setAdminSecurity(Security adminSecurity) {
      this.adminSecurity = adminSecurity;
   }
}
