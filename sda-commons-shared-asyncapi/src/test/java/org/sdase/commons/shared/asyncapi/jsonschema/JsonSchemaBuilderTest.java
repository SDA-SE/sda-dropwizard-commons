/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi.jsonschema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JsonSchemaBuilderTest {

  @Test
  void shouldMergeMultipleResolvedSchemas() {
    JsonSchemaBuilder jsonSchemaBuilder =
        c -> {
          // use existing types to produce test data
          if (c.equals(String.class)) {
            return yamlAsJsonNodeMap(
                """
                Task:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/TaskStatus
                TaskStatus:
                  type: string
                  enum:
                    - todo
                    - done
                """);
          }
          if (c.equals(Number.class)) {
            return yamlAsJsonNodeMap(
                """
                User:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/UserStatus
                UserStatus:
                  type: string
                  enum:
                    - active
                    - disabled
                """);
          }
          return Map.of();
        };

    List<Type> givenTypes = List.of(String.class, Number.class);

    Map<String, JsonNode> actual = jsonSchemaBuilder.toJsonSchema(givenTypes);
    assertThat(actual)
        .isEqualTo(
            yamlAsJsonNodeMap(
                """
            Task:
              type: object
              properties:
                status:
                  $ref: #/components/schemas/TaskStatus
            TaskStatus:
              type: string
              enum:
                - todo
                - done
            User:
              type: object
              properties:
                status:
                  $ref: #/components/schemas/UserStatus
            UserStatus:
              type: string
              enum:
                - active
                - disabled
            """));
  }

  @Test
  void shouldMergeSameDuplicateSchema() {
    JsonSchemaBuilder jsonSchemaBuilder =
        c -> {
          // use existing types to produce test data
          if (c.equals(String.class)) {
            return yamlAsJsonNodeMap(
                """
                Task:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/TaskStatus
                    assignee:
                      $ref: #/components/schemas/UserStatus
                User:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/UserStatus
                UserStatus:
                  type: string
                  enum:
                    - active
                    - disabled
                TaskStatus:
                  type: string
                  enum:
                    - todo
                    - done
                """);
          }
          if (c.equals(Number.class)) {
            return yamlAsJsonNodeMap(
                """
                User:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/UserStatus
                UserStatus:
                  type: string
                  enum:
                    - active
                    - disabled
                """);
          }
          return Map.of();
        };

    List<Type> givenTypes = List.of(String.class, Number.class);

    Map<String, JsonNode> actual = jsonSchemaBuilder.toJsonSchema(givenTypes);
    assertThat(actual)
        .isEqualTo(
            yamlAsJsonNodeMap(
                """
            Task:
              type: object
              properties:
                status:
                  $ref: #/components/schemas/TaskStatus
                assignee:
                  $ref: #/components/schemas/UserStatus
            TaskStatus:
              type: string
              enum:
                - todo
                - done
            User:
              type: object
              properties:
                status:
                  $ref: #/components/schemas/UserStatus
            UserStatus:
              type: string
              enum:
                - active
                - disabled
            """));
  }

  /**
   * This test documents limitations. We may resolve the (or some) conflicts later and remove or
   * adapt this test.
   */
  @Test
  void shouldFailForDuplicateSchema() {
    JsonSchemaBuilder jsonSchemaBuilder =
        c -> {
          // use existing types to produce test data
          if (c.equals(String.class)) {
            return yamlAsJsonNodeMap(
                """
                Task:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/Status
                Status:
                  type: string
                  enum:
                    - todo
                    - done
                """);
          }
          if (c.equals(Number.class)) {
            return yamlAsJsonNodeMap(
                """
                User:
                  type: object
                  properties:
                    status:
                      $ref: #/components/schemas/Status
                Status:
                  type: string
                  enum:
                    - active
                    - disabled
                """);
          }
          return Map.of();
        };

    Set<Type> givenTypes = Set.of(String.class, Number.class);

    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> jsonSchemaBuilder.toJsonSchema(givenTypes))
        .withMessageContaining("Two schema use the the same key:");
  }

  private static Map<String, JsonNode> yamlAsJsonNodeMap(String yamlContent) {
    try {
      return YAMLMapper.builder().build().readValue(yamlContent, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }
}
