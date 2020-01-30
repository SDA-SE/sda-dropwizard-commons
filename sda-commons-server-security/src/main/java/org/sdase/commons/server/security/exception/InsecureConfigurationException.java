package org.sdase.commons.server.security.exception;

/** Exception to be thrown if the configuration looks suspicious. */
public class InsecureConfigurationException extends RuntimeException {

  public InsecureConfigurationException(String message) {
    super(message);
  }
}
