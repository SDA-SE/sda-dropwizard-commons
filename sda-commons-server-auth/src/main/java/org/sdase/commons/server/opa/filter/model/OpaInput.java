package org.sdase.commons.server.opa.filter.model;

import javax.ws.rs.core.MultivaluedMap;

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

  /**
   * Additional, optional headers that get passed to the OPA service.
   */
  private MultivaluedMap<String, String> headers;

  public OpaInput() {
    // nothing here, just for Jackson
  }

  OpaInput(String jwt, String[] path, String httpMethod, String traceToken, MultivaluedMap<String, String> headers) {
    this.jwt = jwt;
    this.path = path;
    this.httpMethod = httpMethod;
    this.trace = traceToken;
    this.headers = headers;
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

  public MultivaluedMap<String, String> getHeaders() {
    return headers;
  }

  public OpaInput setHeaders(MultivaluedMap<String, String> headers) {
    this.headers = headers;
    return this;
  }
}
