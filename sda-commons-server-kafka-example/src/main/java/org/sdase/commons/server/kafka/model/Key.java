package org.sdase.commons.server.kafka.model;

public class Key {

  private String keyInfo;

  public Key() {
    // to allow deserialization
  }

  public Key(String keyInfo) {
    this.keyInfo = keyInfo;
  }

  public String getKeyInfo() {
    return keyInfo;
  }

  public void setKeyInfo(String keyInfo) {
    this.keyInfo = keyInfo;
  }
}
