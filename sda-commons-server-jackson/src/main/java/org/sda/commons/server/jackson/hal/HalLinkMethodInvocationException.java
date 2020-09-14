package org.sda.commons.server.jackson.hal;

public class HalLinkMethodInvocationException extends RuntimeException {

  public HalLinkMethodInvocationException(String message) {
    super(message);
  }

  public HalLinkMethodInvocationException(String message, Throwable cause) {
    super(message, cause);
  }
}
