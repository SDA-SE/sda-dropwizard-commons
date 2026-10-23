package org.sdase.commons.shared.jsonschema;

/** Signals invalid input or a failure while generating a JSON Schema document. */
public class JsonSchemaGenerationException extends RuntimeException {

  public JsonSchemaGenerationException(String message) {
    super(message);
  }

  public JsonSchemaGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
