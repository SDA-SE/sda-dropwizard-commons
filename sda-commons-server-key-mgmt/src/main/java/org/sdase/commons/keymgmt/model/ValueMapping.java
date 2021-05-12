package org.sdase.commons.keymgmt.model;

@SuppressWarnings("unused")
public class ValueMapping {

  private String api;
  private String impl;

  public String getApi() {
    return api;
  }

  public ValueMapping setApi(String api) {
    this.api = api;
    return this;
  }

  public String getImpl() {
    return impl;
  }

  public ValueMapping setImpl(String impl) {
    this.impl = impl;
    return this;
  }
}
