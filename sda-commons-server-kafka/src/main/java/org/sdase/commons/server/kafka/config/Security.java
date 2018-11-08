package org.sdase.commons.server.kafka.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Security {

   @JsonProperty(value = "user")
   private String user;

   @JsonProperty(value = "password")
   private String password;

   @JsonProperty(value = "protocol", defaultValue = "PLAINTEXT")
   private ProtocolType protocol;

   public String getUser() {
      return user;
   }

   public void setUser(String user) {
      this.user = user;
   }

   public String getPassword() {
      return password;
   }

   public void setPassword(String password) {
      this.password = password;
   }

   public ProtocolType getProtocol() {
      return protocol;
   }

   public void setProtocol(ProtocolType protocol) {
      this.protocol = protocol;
   }

}
