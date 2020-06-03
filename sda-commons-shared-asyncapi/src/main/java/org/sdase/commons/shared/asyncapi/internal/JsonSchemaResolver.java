package org.sdase.commons.shared.asyncapi.internal;

import com.fasterxml.jackson.databind.JsonNode;

/** Callback to resolve an external JSON schema to a document. */
public interface JsonSchemaResolver {

  /**
   * Resolve a url to a schema file to a JSON object. Called for every external JSON schema
   * referenced by a schema file.
   *
   * @param url The url to resolve, can be a relative or absolute file, or an url.
   * @return The resolved JSON object, or null if the schema should not be embedded.
   */
  JsonNode resolve(String url);
}
