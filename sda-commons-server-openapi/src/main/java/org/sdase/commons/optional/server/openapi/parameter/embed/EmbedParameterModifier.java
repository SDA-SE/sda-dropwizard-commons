package org.sdase.commons.optional.server.openapi.parameter.embed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** Adds the embeddable resources as query parameter, so they can be selected in the swagger ui. */
@OpenAPIDefinition
@SuppressWarnings({"java:S3740", "rawtypes"})
// ignore "Raw types should not be used" introduced by swagger-core
public class EmbedParameterModifier implements ReaderListener {
  private static final String EMBEDDED_PROPERTY = "_embedded";

  @Override
  public void beforeScan(OpenApiReader reader, OpenAPI openAPI) {
    // nothing to do here
  }

  @Override
  public void afterScan(OpenApiReader reader, OpenAPI openAPI) {
    if (openAPI == null || openAPI.getComponents() == null) {
      return;
    }

    Map<String, Schema> definitions = openAPI.getComponents().getSchemas();
    if (definitions != null && openAPI.getPaths() != null) {
      openAPI
          .getPaths()
          .forEach(
              (key, path) -> path.readOperations().forEach(o -> updateOperations(o, definitions)));
    }
  }

  private void updateOperations(Operation operation, Map<String, Schema> definitions) {
    operation
        .getResponses()
        .forEach(
            (key, response) -> {
              if (response.getContent() == null
                  || !response.getContent().containsKey(APPLICATION_JSON)) {
                return;
              }

              String responseModelName =
                  getResponseModelName(response.getContent().get(APPLICATION_JSON));
              if (responseModelName == null) {
                return;
              }

              Schema<?> schemaDefinition = getSchemaDefinition(definitions, responseModelName);
              if (schemaDefinition == null) {
                return;
              }

              // In a search endpoint, the resource that supports embedding might be in a
              // list-property.
              if (schemaDefinition.getProperties() != null
                  && !schemaDefinition.getProperties().containsKey(EMBEDDED_PROPERTY)) {
                schemaDefinition = getResolvedSchemaDefinition(definitions, schemaDefinition);
              }

              Map<String, Schema> embeddedProperties = getEmbeddedObjectProperty(schemaDefinition);

              Parameter embedQueryParameter = getEmbedQueryParameter(embeddedProperties);
              if (embedQueryParameter == null) {
                return;
              }

              operation.addParametersItem(embedQueryParameter);
            });
  }

  String getOriginalRef(Schema<?> schema) {
    if (schema != null && schema.get$ref() != null) {
      return schema.get$ref().replaceAll("^#/components/schemas/(.*)", "$1");
    }

    return null;
  }

  private String getResponseModelName(MediaType responseSchema) {
    return getOriginalRef(responseSchema.getSchema());
  }

  private Schema<?> getSchemaDefinition(Map<String, Schema> definitions, String name) {
    return definitions.get(name);
  }

  private Schema<?> getResolvedSchemaDefinition(
      Map<String, Schema> definitions, Schema<?> definition) {
    List<String> nestedRefs =
        definition.getProperties().values().stream()
            // should be an array
            .filter(ArraySchema.class::isInstance)

            // should have an item that is a reference to a definition
            .map(es -> ((ArraySchema) es).getItems())
            .filter(Objects::nonNull)

            // get the model reference name
            .map(this::getOriginalRef)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

    // only when there is a single list entry that supports embedding
    if (nestedRefs.size() == 1) {
      // get the model definition from the array
      return getSchemaDefinition(definitions, nestedRefs.get(0));
    }

    return definition;
  }

  private Map<String, Schema> getEmbeddedObjectProperty(Schema<?> definition) {
    HashMap<String, Schema> allProperties = new HashMap<>();

    // check if it is a composed schema
    if (definition instanceof ComposedSchema) {
      // use the properties of every element in allOf. we ignore anyOf.
      List<Schema> allOf = ((ComposedSchema) definition).getAllOf();
      if (allOf != null) {
        allOf.forEach(s -> allProperties.putAll(getEmbeddedObjectProperty(s)));
      }
    }

    // add all properties from a schema
    if (definition.getProperties() != null) {
      Schema<?> schema = definition.getProperties().get(EMBEDDED_PROPERTY);
      if (schema != null) {
        allProperties.putAll(schema.getProperties());
      }
    }

    return allProperties;
  }

  private Parameter getEmbedQueryParameter(Map<String, Schema> properties) {
    if (properties != null && !properties.isEmpty()) {
      List<String> embeddableObjects =
          properties.keySet().stream().sorted().collect(Collectors.toList());

      return new QueryParameter()
          .schema(new ArraySchema().items(new StringSchema()._enum(embeddableObjects)))
          .name("embed")
          .description(
              "Select linked resources that should be resolved and embedded into the response");
    }

    return null;
  }
}
