package org.sdase.commons.shared.asyncapi;

/**
 * @deprecated This class will be moved to server-testing with the next major update. It is assumed
 *     that no direct access from consumers is required. In case this assumption is wrong, a
 *     migration path will be provided with the next major update. Reach out to the maintainers in
 *     case migrating with the next major is not an option.
 */
@Deprecated(forRemoval = true)
public class UnknownSchemaException extends RuntimeException {

  public UnknownSchemaException(String message) {
    super(message);
  }
}
