package org.sdase.commons.optional.server.openapi.parameter.embed;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import io.swagger.v3.jaxrs2.ReaderListener;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.integration.api.OpenApiReader;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adds the embeddable resources as query parameter, so they can be selected in the swagger ui. */
@OpenAPIDefinition
@SuppressWarnings({"java:S3740", "rawtypes"})
// ignore "Raw types should not be used" introduced by swagger-core
public class EmbedParameterModifier implements ReaderListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(EmbedParameterModifier.class);

  private static final String EMBEDDED_PROPERTY = "_embedded";
  private static final String EMBED_PARAMETER_NAME = "embed";
  private static final String QUERY_PARAMETER_LOCATION = "query";

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
              addEmbedParameter(operation, embedQueryParameter);
            });
  }

  String getOriginalRef(Schema<?> schema) {
    if (schema != null && schema.get$ref() != null) {
      return schema.get$ref().replaceAll("^#/components/schemas/(.*)", "$1");
    }

    return null;
  }

  private void addEmbedParameter(Operation operation, Parameter generatedParameter) {
    Parameter existingEmbedParameter = null;

    if (operation.getParameters() != null) {
      for (Parameter parameter : operation.getParameters()) {
        if (parameter == null || !isEmbedQueryParameter(parameter)) {
          continue;
        }

        if (existingEmbedParameter != null) {
          throw new IllegalStateException(
              "The operation contains multiple 'embed' query parameters already.");
        }

        existingEmbedParameter = parameter;
      }
    }

    if (existingEmbedParameter == null) {
      operation.addParametersItem(generatedParameter);
      return;
    }

    if (!isCompatibleEmbedParameter(existingEmbedParameter)) {
      throw new IllegalStateException(
          "The query parameter 'embed' conflicts with the generated embed parameter. "
              + "It must be an array of strings or use a different name.");
    }

    LOGGER.info(
        "A compatible 'embed' query parameter already exists for operation '{}'. "
            + "Adding another one was skipped.",
        operation.getOperationId());
  }

  private boolean isEmbedQueryParameter(Parameter parameter) {
    return EMBED_PARAMETER_NAME.equals(parameter.getName())
        && QUERY_PARAMETER_LOCATION.equals(parameter.getIn());
  }

  private boolean isCompatibleEmbedParameter(Parameter parameter) {
    Schema<?> schema = parameter.getSchema();

    if (schema == null || !"array".equals(schema.getType())) {
      return false;
    }

    Schema<?> itemSchema = schema.getItems();
    return itemSchema != null && "string".equals(itemSchema.getType());
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
            .toList();

    // normally a search result model contains besides other meta information a list with the
    // initially passed filters and a list with the search results therefore the correct list must
    // be filtered
    var schemaDefinition =
        nestedRefs.stream()
            // map refs to model definition
            .map(schemaRef -> getSchemaDefinition(definitions, schemaRef))
            // filter the model definition with embedded properties
            .filter(
                schema ->
                    schema.getProperties() != null
                        && schema.getProperties().containsKey(EMBEDDED_PROPERTY))
            .findFirst();
    if (schemaDefinition.isPresent()) {
      return schemaDefinition.get();
    }

    return definition;
  }

  private Map<String, Schema> getEmbeddedObjectProperty(Schema<?> definition) {
    HashMap<String, Schema> allProperties = new HashMap<>();

    // check if it is a composed schema
    if (definition instanceof ComposedSchema composedSchema) {
      // use the properties of every element in allOf. we ignore anyOf.
      List<Schema> allOf = composedSchema.getAllOf();
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
      List<String> embeddableObjects = properties.keySet().stream().sorted().toList();

      return new QueryParameter()
          .schema(new ArraySchema().items(new StringSchema()._enum(embeddableObjects)))
          .name(EMBED_PARAMETER_NAME)
          .description(
              "Select linked resources that should be resolved and embedded into the response");
    }

    return null;
  }
}
