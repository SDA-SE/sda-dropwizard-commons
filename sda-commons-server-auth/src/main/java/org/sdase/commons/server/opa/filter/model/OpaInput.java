package org.sdase.commons.server.opa.filter.model;

public class OpaInput {

  /**
   * trace token to be able to find opa debug
   */
  private String trace;

  /**
   * JWT received with the request
   */
  private String jwt;

  /**
   * url path to the resource without base url
   */
  private String[] path;

  /**
   * HTTP Method
   */
  private String httpMethod;

  public OpaInput() {
    // nothing here, just for Jackson
  }

  OpaInput(String jwt, String[] path, String httpMethod, String traceToken) {
    this.jwt = jwt;
    this.path = path;
    this.httpMethod = httpMethod;
    this.trace = traceToken;
  }

  public String getJwt() {
    return jwt;
  }

  public OpaInput setJwt(String jwt) {
    this.jwt = jwt;
    return this;
  }

  public String[] getPath() {
    return path;
  }

  public OpaInput setPath(String[] path) {
    this.path = path;
    return this;
  }

  public String getHttpMethod() {
    return httpMethod;
  }

  public OpaInput setHttpMethod(String httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  public String getTrace() {
    return trace;
  }

  public OpaInput setTrace(String trace) {
    this.trace = trace;
    return this;
  }
}
