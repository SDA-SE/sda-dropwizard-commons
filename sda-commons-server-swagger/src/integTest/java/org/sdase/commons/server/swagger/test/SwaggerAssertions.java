package org.sdase.commons.server.swagger.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.github.fge.jsonschema.main.JsonValidator;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import javax.ws.rs.core.Response;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

public final class SwaggerAssertions {

   public static void assertValidSwagger2Yaml(Response response) {
      InputStream entity = response.readEntity(InputStream.class);

      try {
         YamlHolder.yaml.load(entity);
      } catch (YAMLException e) {
         fail("response cannot be converted to YAML", e);
      }
   }

   public static void assertValidSwagger2Json(Response response) {
      String entity = response.readEntity(String.class);

      JsonNode swagger = null;
      try {
         swagger = JsonLoader.fromString(entity);
      } catch (IOException e) {
         fail("response cannot be converted to JSON", e);
      }

      try {
         ProcessingReport report = JsonValidatorHolder.jsonValidator
               .validate(SwaggerSchemaHolder.swagger2Schema, swagger);

         assertThat(report.isSuccess()).isTrue();
      } catch (ProcessingException e) {
         fail("response cannot be validated", e);
      }
   }

   private SwaggerAssertions() {
      // prevent instantiation
   }

   private static class YamlHolder {

      static final Yaml yaml = createYaml();

      private static Yaml createYaml() {
         return new Yaml();
      }
   }

   private static class JsonValidatorHolder {

      static final JsonValidator jsonValidator = createJsonValidator();

      private static JsonValidator createJsonValidator() {
         return JsonSchemaFactory.byDefault().getValidator();
      }
   }

   private static class SwaggerSchemaHolder {

      static final JsonNode swagger2Schema = loadSwagger2Schema();

      private static final String SWAGGER_2_0_SCHEMA_JSON_LOCATION = "/swagger2.0-schema.json";

      private static JsonNode loadSwagger2Schema() {
         try {
            return JsonLoader.fromResource(SWAGGER_2_0_SCHEMA_JSON_LOCATION);
         } catch (IOException e) {
            throw new IllegalStateException(
                  String.format(Locale.ROOT, "cannot load '%s'", SWAGGER_2_0_SCHEMA_JSON_LOCATION),
                  e);
         }
      }
   }
}
