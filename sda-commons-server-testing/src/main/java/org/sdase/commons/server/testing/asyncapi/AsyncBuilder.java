package org.sdase.commons.server.testing.asyncapi;

import com.fasterxml.jackson.databind.JsonNode;
import org.sdase.commons.shared.asyncapi.jsonschema.JsonSchemaBuilder;

@SuppressWarnings("java:S2176") // temporary issue until shared.asyncapi.AsyncBuilder is removed
public class AsyncBuilder extends org.sdase.commons.shared.asyncapi.AsyncBuilder
    implements AsyncApiGenerator.AsyncApiBaseBuilder,
        AsyncApiGenerator.SchemaBuilder,
        FinalBuilder {

  // next-major: move code from org.sdase.commons.shared.asyncapi.AsyncBuilder to here

  AsyncBuilder() {
    super();
  }

  @Override
  public AsyncApiGenerator.SchemaBuilder withAsyncApiBase(String yamlAsyncApiContent) {
    super.withAsyncApiBase(yamlAsyncApiContent);
    return this;
  }

  @Override
  public FinalBuilder withJsonSchemaBuilder(JsonSchemaBuilder jsonSchemaBuilder) {
    super.withJsonSchemaBuilder(jsonSchemaBuilder);
    return this;
  }

  @Override
  public JsonNode generate() {
    return super.generate();
  }
}
