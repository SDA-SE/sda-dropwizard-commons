package org.sdase.commons.server.mongo;

import org.hibernate.validator.constraints.NotEmpty;

public class MongoConfiguration  {

   @NotEmpty
   private String database;

   @NotEmpty
   private String hosts;

   private String options = "";

   private String username;

   private String password;

   private boolean isSSL;

   // In the original implementation the certifcate must be placed in an environment variable
   // this can be done by using placeholders in the config now
   private String certificate;

   public String getDatabase() {
      return database;
   }

   public void setDatabase(String database) {
      this.database = database;
   }

   public String getHosts() {
      return hosts;
   }

   public void setHosts(String hosts) {
      this.hosts = hosts;
   }

   public String getOptions() {
      return options;
   }

   public void setOptions(String options) {
      this.options = options;
   }

   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public boolean isSSL() {
      return isSSL;
   }

   public void setSSL(boolean SSL) {
      isSSL = SSL;
   }

   public String getCertificate() {
      return certificate;
   }

   public void setCertificate(String certificate) {
      this.certificate = certificate;
   }
}
