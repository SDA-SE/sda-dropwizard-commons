package org.sdase.commons.shared.asyncapi;

/**
 * Exception to be thrown when a class referenced as {@code $ref:
 * class://fully.qualified.classname.of.Model} in an AsyncAPI is not found in the classpath.
 *
 * @deprecated This class will be moved to server-testing with the next major update. It is assumed
 *     that no direct access from consumers is required. In case this assumption is wrong, a
 *     migration path will be provided with the next major update. Reach out to the maintainers in
 *     case migrating with the next major is not an option.
 */
@Deprecated(forRemoval = true)
public class ReferencedClassNotFoundException extends RuntimeException {

  public ReferencedClassNotFoundException(ClassNotFoundException e) {
    super(e);
  }
}
