package org.sdase.commons.optional.server.swagger.json.example;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.jaxrs.Reader;
import io.swagger.jaxrs.config.ReaderListener;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import io.swagger.models.properties.Property;
import java.io.IOException;
import java.util.Map.Entry;

/**
 * Converts examples from the sagger annotations from simple strings into JSON
 * to allow to use the full expressiveness of swagger.
 */
@SwaggerDefinition
public class JsonExampleModifier implements ReaderListener {

   @Override
   public void beforeScan(Reader reader, Swagger swagger) {
      // nothing to do here
   }

   @Override
   public void afterScan(Reader reader, Swagger swagger) {
      if (swagger == null) {
         return;
      }
      if (swagger.getDefinitions() != null) {
         swagger.getDefinitions().forEach((key, model) -> updateModelExample(model));
      }
      if (swagger.getPaths() != null) {
         swagger.getPaths().forEach((key, path) -> path.getOperations().forEach(this::updateOperations));
      }
   }

   private void updateOperations(Operation operation) {
      operation.getResponses().forEach((key, response) -> response.setExamples(
            response.getExamples().entrySet().stream().collect(toMap(Entry::getKey, v -> asJson(v.getValue())))));
   }

   private void updateModelExample(Model model) {
      if (model.getProperties() != null) {
         model.getProperties().forEach((name, property) -> updatePropertyExample(property));
      }

      if (model instanceof ComposedModel) {
         // Composed models (using inheritance) behave a bit different, the properties can be null
         // and the real properties are located in the child.
         updateModelExample(((ComposedModel)model).getChild());
      }

      model.setExample(asJson(model.getExample()));
   }

   private void updatePropertyExample(Property property) {
      property.setExample(asJson(property.getExample()));
   }

   private Object asJson(Object possibleJson) {
      // Try to parse the object as json, if possible the json value is used, if
      // it isn't possible the original value as a string is used. This is more
      // or less backward compatible, but fixing existing issues like numbers
      // that are strings. To force numbers (booleans, null) into strings, use
      // double quotes.
      if (possibleJson instanceof String) {
         String jsonString = (String) possibleJson;
         ObjectMapper mapper = new ObjectMapper();
         JsonFactory factory = mapper.getFactory();
         try {
            JsonParser parser = factory.createParser(jsonString);
            return mapper.readTree(parser);
         } catch (IOException e) {
            return jsonString;
         }
      } else {
         return possibleJson;
      }
   }
}
