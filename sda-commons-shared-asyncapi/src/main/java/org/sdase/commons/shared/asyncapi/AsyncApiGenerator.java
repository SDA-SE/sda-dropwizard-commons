package org.sdase.commons.shared.asyncapi;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.sdase.commons.shared.asyncapi.jsonschema.JsonSchemaBuilder;

/**
 * Generator used to build AsyncAPI specs from a template base file and schemas generated from code.
 * The schemas are referenced via custom class {@code $ref}erences in an AsyncAPI yaml template like
 * at the very end of the following example AsyncAPI:
 *
 * <pre>
 *   asyncapi: '2.5.0'
 *   id: urn:com:example:entity
 *   defaultContentType: application/json
 *   info:
 *     title: Example App
 *     description: This example is about events of an example.
 *     version: '1.0.0'
 *
 *   channels:
 *     'partner-created':
 *       publish:
 *         operationId: publishPartnerCreatedEvents
 *         summary: Partner Created Events
 *         description: A new partner was created
 *         message:
 *           oneOf:
 *             - $ref: '#/components/messages/SomeEvent'
 *
 *   components:
 *     messages:
 *       SomeEvent:
 *         title: Some Event
 *         description: Something happened.
 *         payload:
 *           # Reference to a class in the classpath.
 *           # The reference will by replaced by the local path of the generated schema, e.g.
 *           # $ref: '#/components/schemas/SomeEvent'
 *           $ref: class://com.example.entity.model.SomeEvent
 * </pre>
 *
 * These referenced files are automatically created from Java classes annotated with Jackson,
 * Swagger and Jakarta Validation annotations. Afterwards everything is embedded into a single
 * self-contained AsyncAPI yaml file.
 */
public class AsyncApiGenerator {

  private AsyncApiGenerator() {
    // No public constructor
  }

  /**
   * Creates a new generator for AsyncAPI specs
   *
   * @return builder
   */
  public static AsyncApiBaseBuilder builder() {
    return new AsyncBuilder();
  }

  public interface AsyncApiBaseBuilder {

    /**
     * Supply a base AsyncAPI file to be used as a template
     *
     * @param url The resource url to the template file
     * @return builder
     * @throws UncheckedIOException when reading the url as yaml failed
     */
    default SchemaBuilder withAsyncApiBase(URL url) {
      try (var is = url.openStream()) {
        return withAsyncApiBase(new String(is.readAllBytes(), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new UncheckedIOException("Error reading content from %s".formatted(url), e);
      }
    }

    /**
     * Supply base AsyncAPI yaml content to be used as a template.
     *
     * @param yamlAsyncApiContent The content of an AsyncAPI yaml schema.
     * @return builder
     * @throws UncheckedIOException when parsing as yaml failed
     */
    SchemaBuilder withAsyncApiBase(String yamlAsyncApiContent);
  }

  public interface SchemaBuilder extends FinalBuilder {

    /**
     * @param jsonSchemaBuilder the {@link JsonSchemaBuilder} that creates Json Schema Draft 07 for
     *     classes referenced as {@code $ref: class://fully.qualified.classname.of.the.Model}.
     * @return The {@code FinalBuilder} to create the AsyncAPI
     */
    FinalBuilder withJsonSchemaBuilder(JsonSchemaBuilder jsonSchemaBuilder);
  }
}
