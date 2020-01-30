package org.sdase.commons.shared.yaml;

public class TestBean2 {

  static final String TEST_STRING = "TESTBEAN2";

  String message = TEST_STRING;
  String attribute;
  String id;

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getAttribute() {
    return attribute;
  }

  public void setAttribute(String attribute) {
    this.attribute = attribute;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }
}
