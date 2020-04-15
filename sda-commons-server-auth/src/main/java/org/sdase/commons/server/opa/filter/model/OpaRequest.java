package org.sdase.commons.server.opa.filter.model;

import com.fasterxml.jackson.databind.JsonNode;

public class OpaRequest {

  private JsonNode input;

  public OpaRequest() {
    // nothing here
  }

  private OpaRequest(JsonNode input) {
    this.input = input;
  }

  public JsonNode getInput() {
    return input;
  }

  public OpaRequest setInput(JsonNode input) {
    this.input = input;
    return this;
  }

  public static OpaRequest request(JsonNode input) {
    return new OpaRequest(input);
  }
}
