package org.sdase.commons.server.kafka.config;

import java.util.ArrayList;
import java.util.List;

public class AdminConfig {
	
   private int adminClientRequestTimeoutMs = 5000;
	   
   private List<String> adminEndpoint = new ArrayList<>();
   
   private Security adminSecurity = new Security();

   public int getAdminClientRequestTimeoutMs() {
      return adminClientRequestTimeoutMs;
   }

   public void setAdminClientRequestTimeoutMs(int adminClientRequestTimeoutMs) {
      this.adminClientRequestTimeoutMs = adminClientRequestTimeoutMs;
   }

   public List<String> getAdminEndpoint() {
      return adminEndpoint;
   }

   public void setAdminEndpoint(List<String> adminEndpoint) {
      this.adminEndpoint = adminEndpoint;
   }

   public Security getAdminSecurity() {
      return adminSecurity;
   }

   public void setAdminSecurity(Security adminSecurity) {
      this.adminSecurity = adminSecurity;
   }
}
