/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.shared.asyncapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.testing.GoldenFileAssertions;
import org.sdase.commons.shared.asyncapi.AsyncApiGenerator.SchemaBuilder;

class AsyncApiGeneratorTest {

  public static final String GIVEN_ASYNC_API_TEMPLATE =
      "/AsyncApiGeneratorTest/asyncapi_template.yaml";

  @Test
  void shouldGenerateAsyncApi() throws IOException {
    String actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource(GIVEN_ASYNC_API_TEMPLATE))
            .generateYaml();

    GoldenFileAssertions.assertThat(
            Path.of("src/test/resources/AsyncApiGeneratorTest/asyncapi_expected.yaml"))
        .hasYamlContentAndUpdateGolden(actual);
  }

  @Test
  void shouldSortSchemas() {
    JsonNode actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource(GIVEN_ASYNC_API_TEMPLATE))
            .generate();
    JsonNode schemas = actual.at("/components/schemas");
    List<String> keys = new ArrayList<>();
    schemas.fieldNames().forEachRemaining(keys::add);
    // usingRecursiveComparison() is unable to compare the order, so we have to do it manually.
    assertThat(keys).isSorted();
  }

  @Test
  void shouldUseCustomSchemaGenerator() {
    ObjectMapper om = new ObjectMapper();
    JsonNode actual =
        AsyncApiGenerator.builder()
            .withAsyncApiBase(getClass().getResource(GIVEN_ASYNC_API_TEMPLATE))
            .withJsonSchemaBuilder(
                c -> Map.of(((Class<?>) c).getSimpleName() + "Custom", om.createObjectNode()))
            .generate();
    assertThat(actual.get("components").get("schemas").fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder("CarManufacturedCustom", "CarScrappedCustom");
  }

  @Test
  void shouldFailIfRefIsNotFound() {
    URL asyncApiWithBadReferences =
        getClass().getResource("/AsyncApiGeneratorTest/asyncapi_reference_failure_template.yaml");
    SchemaBuilder builderForSchemaWithBadReferences =
        AsyncApiGenerator.builder().withAsyncApiBase(asyncApiWithBadReferences);
    assertThatExceptionOfType(ReferencedClassNotFoundException.class)
        .isThrownBy(builderForSchemaWithBadReferences::generate)
        .withMessageContaining("com.example.CarManufactured");
  }
}
