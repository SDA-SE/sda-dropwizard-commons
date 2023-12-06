package org.sdase.commons.shared.asyncapi;

/**
 * Exception to be thrown when a class referenced as {@code $ref:
 * class://fully.qualified.classname.of.Model} in an AsyncAPI is not found in the classpath.
 */
public class ReferencedClassNotFoundException extends RuntimeException {

  public ReferencedClassNotFoundException(ClassNotFoundException e) {
    super(e);
  }
}
