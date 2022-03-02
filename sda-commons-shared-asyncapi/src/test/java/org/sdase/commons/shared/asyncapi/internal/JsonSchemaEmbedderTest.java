package org.sdase.commons.shared.asyncapi.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import org.sdase.commons.shared.yaml.YamlUtil;

public class JsonSchemaEmbedderTest {

  @Test
  public void shouldEmbedObject() {
    JsonNode input =
        YamlUtil.load(
            getClass().getResource("/json_schema_embedder/simple_object_input.yaml"),
            JsonNode.class);
    JsonSchemaEmbedder embedder =
        new JsonSchemaEmbedder(
            "/definitions",
            name ->
                YamlUtil.load(
                    getClass().getResource("/json_schema_embedder/" + name), JsonNode.class));
    JsonNode result = embedder.resolve(input);

    assertThat(result.at("/definitions").fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder(
            "Person", "Address", "Country", "simpleobjectsimplereferencedyaml");
    assertThat(result.at("/definitions/Person/properties/address/$ref").asText())
        .isEqualTo("#/definitions/Address");
    assertThat(result.at("/definitions/Address/properties/country/$ref").asText())
        .isEqualTo("#/definitions/Country");
    assertThat(result.at("/definitions/simpleobjectsimplereferencedyaml/$schema").isMissingNode())
        .isTrue();
  }

  @Test
  public void shouldNotEmbedObjectsAsURLs() {
    JsonNode input =
        YamlUtil.load(
            getClass().getResource("/json_schema_embedder/simple_object_url.yaml"), JsonNode.class);
    JsonSchemaEmbedder embedder =
        new JsonSchemaEmbedder(
            "/definitions",
            name ->
                YamlUtil.load(
                    getClass().getResource("/json_schema_embedder/" + name), JsonNode.class));
    JsonNode result = embedder.resolve(input);

    assertThat(result.at("/definitions").fieldNames())
        .toIterable()
        .containsExactlyInAnyOrder("Person", "simpleobjectsimplereferencedyaml");
    assertThat(result.at("/definitions/simpleobjectsimplereferencedyaml").isMissingNode())
        .isFalse();
    assertThat(result.at("/definitions/address").isMissingNode()).isTrue();
    assertThat(result.at("/definitions/Person/properties/address/$ref").asText())
        .isEqualTo("https://sda.se");
  }
}
