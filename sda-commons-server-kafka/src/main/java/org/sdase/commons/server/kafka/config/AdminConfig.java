package org.sdase.commons.server.kafka.config;

import java.util.ArrayList;
import java.util.List;

public class AdminConfig {
	
   private int adminClientRequestTimeoutMs = 5000;
	   
   private List<String> adminRestApi = new ArrayList<>();
   
   private Security adminSecurity = new Security();

   public int getAdminClientRequestTimeoutMs() {
      return adminClientRequestTimeoutMs;
   }

   public void setAdminClientRequestTimeoutMs(int adminClientRequestTimeoutMs) {
      this.adminClientRequestTimeoutMs = adminClientRequestTimeoutMs;
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
