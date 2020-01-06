package org.sdase.commons.server.opa.filter.model;

import javax.ws.rs.core.MultivaluedMap;

public class OpaRequest {

  private OpaInput input;

  public OpaRequest() {
    // nothing here
  }

  private OpaRequest(OpaInput input) {
    this.input = input;
  }


  public OpaInput getInput() {
    return input;
  }

  public OpaRequest setInput(OpaInput input) {
    this.input = input;
    return this;
  }

  public static OpaRequest request(String jwt, String[] path, String method, String traceToken,
                                   MultivaluedMap<String, String> headers) {
    return new OpaRequest(new OpaInput(jwt, path, method, traceToken, headers));
  }

}
