package org.sdase.commons.server.spring.data.mongo.compatibility.model;

public enum MyEnum {
  A,
  B,
  C;

  @Override
  public String toString() {
    return this.name() + " as String";
  }
}
