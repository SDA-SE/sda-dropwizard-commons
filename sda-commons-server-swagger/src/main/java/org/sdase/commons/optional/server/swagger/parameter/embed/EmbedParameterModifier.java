package org.sdase.commons.optional.server.swagger.parameter.embed;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Adds the embeddable resources as query parameter such that they can be selected in swagger. */
@SwaggerDefinition
public class EmbedParameterModifier implements ReaderListener {
  private static final String EMBEDDED_PROPERTY = "_embedded";

  @Override
  public void beforeScan(Reader reader, Swagger swagger) {
    // nothing to do here
  }

  @Override
  public void afterScan(Reader reader, Swagger swagger) {
    if (swagger == null) {
      return;
    }

    Map<String, Model> definitions = swagger.getDefinitions();
    if (definitions != null && swagger.getPaths() != null) {
      swagger
          .getPaths()
          .forEach(
              (key, path) -> path.getOperations().forEach(o -> updateOperations(o, definitions)));
    }
  }

  private void updateOperations(Operation operation, Map<String, Model> definitions) {
    operation
        .getResponses()
        .forEach(
            (key, response) -> {
              String responseModelName = getResponseModelName(response.getResponseSchema());
              if (responseModelName == null) {
                return;
              }

              Model modelDefinition = getModelDefinition(definitions, responseModelName);
              if (modelDefinition == null) {
                return;
              }

              // In a search endpoint, the resource that supports embedding might be in a
              // list-property.
              if (!modelDefinition.getProperties().containsKey(EMBEDDED_PROPERTY)) {
                modelDefinition = getResolvedModelDefinition(definitions, modelDefinition);
              }

              ObjectProperty embedded = getEmbeddedObjectProperty(modelDefinition);
              if (embedded == null) {
                return;
              }

              QueryParameter embedQueryParameter = getEmbedQueryParameter(embedded);
              if (embedQueryParameter == null) {
                return;
              }

              operation.addParameter(embedQueryParameter);
            });
  }

  private String getResponseModelName(Model responseSchema) {
    if (responseSchema instanceof RefModel) {
      RefModel model = (RefModel) responseSchema;
      return model.getOriginalRef();
    }

    return null;
  }

  private Model getModelDefinition(Map<String, Model> definitions, String name) {
    if (definitions.containsKey(name)) {
      Model definition = definitions.get(name);

      if (definition instanceof ModelImpl) {
        return definition;
      } else if (definition instanceof ComposedModel) {
        return ((ComposedModel) definition).getChild();
      }
    }

    return null;
  }

  private Model getResolvedModelDefinition(Map<String, Model> definitions, Model definition) {
    List<String> nestedRefs =
        definition.getProperties().values().stream()
            // should be an array
            .filter(es -> es instanceof ArrayProperty)

            // should have an item that is a reference to a definition
            .map(es -> ((ArrayProperty) es).getItems())
            .filter(p -> p instanceof RefProperty)

            // get the model reference name
            .map(p -> ((RefProperty) p).getOriginalRef())
            .collect(Collectors.toList());

    // only when there is a single list entry that supports embedding
    if (nestedRefs.size() == 1) {
      // get the model definition from the array
      return getModelDefinition(definitions, nestedRefs.get(0));
    }

    return definition;
  }

  private ObjectProperty getEmbeddedObjectProperty(Model definition) {
    if (definition.getProperties() != null
        && definition.getProperties().containsKey(EMBEDDED_PROPERTY)) {
      Property embedded = definition.getProperties().get(EMBEDDED_PROPERTY);

      if (embedded instanceof ObjectProperty) {
        return (ObjectProperty) embedded;
      }
    }

    return null;
  }

  private QueryParameter getEmbedQueryParameter(ObjectProperty embeddedObjectProperty) {
    if (embeddedObjectProperty.getProperties() != null) {
      List<String> embeddableObjects =
          embeddedObjectProperty.getProperties().keySet().stream()
              .sorted()
              .collect(Collectors.toList());

      StringProperty stringProperty = new StringProperty();
      stringProperty.setEnum(embeddableObjects);

      return new QueryParameter()
          .type("array")
          .collectionFormat("multi")
          .name("embed")
          .description(
              "Select linked resources that should be resolved and embedded into the response")
          .collectionFormat("multi")
          .items(stringProperty);
    }

    return null;
  }
}
