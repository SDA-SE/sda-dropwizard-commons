package com.sdase.commons.server.kafka.config;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Broker {

   @NotNull
   @JsonProperty("server")
   private String server;

   @NotNull
   @JsonProperty("port")
   private Integer port;

   @Override
   public String toString() {
      return server.concat(":").concat(port.toString());
   }

   public String getServer() {
      return server;
   }

   public void setServer(String server) {
      this.server = server;
   }

   public Integer getPort() {
      return port;
   }

   public void setPort(Integer port) {
      this.port = port;
   }

}
