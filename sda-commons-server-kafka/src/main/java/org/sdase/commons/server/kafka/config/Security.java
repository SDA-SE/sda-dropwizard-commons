package org.sdase.commons.server.kafka.config;

public class Security {

  private String user;

  private String password;

  private ProtocolType protocol;

  private String saslMechanism = "PLAIN";

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

  public String getSaslMechanism() {
    return saslMechanism;
  }

  public void setSaslMechanism(String saslMechanism) {
    this.saslMechanism = saslMechanism;
  }
}
