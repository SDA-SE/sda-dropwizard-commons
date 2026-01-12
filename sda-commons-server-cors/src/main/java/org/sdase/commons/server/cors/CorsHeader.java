package org.sdase.commons.server.cors;

public class CorsHeader {

  // Request headers
  public static final String ORIGIN_HEADER = "Origin";
  public static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER = "Access-Control-Request-Method";
  public static final String ACCESS_CONTROL_REQUEST_HEADERS_HEADER =
      "Access-Control-Request-Headers";
  // Response headers
  public static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";
  public static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER = "Access-Control-Allow-Methods";
  public static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER = "Access-Control-Allow-Headers";
  public static final String ACCESS_CONTROL_MAX_AGE_HEADER = "Access-Control-Max-Age";
  public static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER =
      "Access-Control-Allow-Credentials";
  public static final String ACCESS_CONTROL_EXPOSE_HEADERS_HEADER = "Access-Control-Expose-Headers";
  public static final String TIMING_ALLOW_ORIGIN_HEADER = "Timing-Allow-Origin";

  private CorsHeader() {}
}
