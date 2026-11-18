package org.sdase.commons.server.testing.asyncapi;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.dataformat.yaml.YAMLMapper;

public interface FinalBuilder {

  /**
   * Generates a new JSON schema for the supplied class.
   *
   * @return A JSON object for the JSON schema.
   */
  JsonNode generate();

  /**
   * Generates a new JSON schema for the supplied class.
   *
   * @return A YAML representation for the JSON schema.
   * @throws JacksonException if json processing fails
   */
  default String generateYaml() throws JacksonException {
    return YAMLMapper.builder().build().writeValueAsString(generate());
  }
}
