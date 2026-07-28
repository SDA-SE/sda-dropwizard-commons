package org.sdase.commons.optional.server.openapi.parameter.embed;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.QueryParameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Test;

class EmbedParameterModifierTest {

  private static final String RESOURCE_SCHEMA_NAME = "TestResource";
  private static final String EMBEDDABLE_PROPERTY_NAME = "relatedResources";
  private static final String TEST_PATH = "/resources/{id}";

  @Test
  void shouldReturnNullForNullSchemas() {
    assertThat(new EmbedParameterModifier().getOriginalRef(null)).isNull();
  }

  @Test
  void shouldAddEmbedParameterWhenItDoesNotExist() {
    Operation operation = new Operation();
    OpenAPI openApi = createOpenApi(operation);

    new EmbedParameterModifier().afterScan(null, openApi);

    assertThat(operation.getParameters())
        .singleElement()
        .satisfies(
            parameter -> {
              assertThat(parameter.getName()).isEqualTo("embed");
              assertThat(parameter.getIn()).isEqualTo("query");
              assertThat(parameter.getSchema()).isInstanceOf(ArraySchema.class);

              Schema<?> itemSchema = parameter.getSchema().getItems();

              assertThat(itemSchema)
                  .isInstanceOfSatisfying(
                      StringSchema.class,
                      stringSchema ->
                          assertThat(stringSchema.getEnum())
                              .containsExactly(EMBEDDABLE_PROPERTY_NAME));
            });
  }

  @Test
  void shouldKeepCompatibleExistingEmbedParameter() {
    Parameter existingParameter =
        new QueryParameter()
            .name("embed")
            .description("Parameter from imported specification")
            .required(false)
            .style(Parameter.StyleEnum.FORM)
            .explode(true)
            .schema(new ArraySchema().items(new StringSchema()));

    Operation operation = new Operation().addParametersItem(existingParameter);
    OpenAPI openApi = createOpenApi(operation);

    new EmbedParameterModifier().afterScan(null, openApi);

    assertThat(operation.getParameters()).containsExactly(existingParameter);
  }

  @Test
  void shouldRejectIncompatibleExistingEmbedParameter() {
    Parameter existingParameter = new QueryParameter().name("embed").schema(new BooleanSchema());

    Operation operation = new Operation().addParametersItem(existingParameter);
    OpenAPI openApi = createOpenApi(operation);

    EmbedParameterModifier modifier = new EmbedParameterModifier();

    assertThatThrownBy(() -> modifier.afterScan(null, openApi))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage(
            "The query parameter 'embed' conflicts with the generated embed parameter. "
                + "It must be an array of strings or use a different name.");
  }

  @Test
  void shouldRejectMultipleExistingEmbedParameters() {
    Parameter firstEmbedParameter =
        new QueryParameter().name("embed").schema(new ArraySchema().items(new StringSchema()));

    Parameter secondEmbedParameter =
        new QueryParameter().name("embed").schema(new ArraySchema().items(new StringSchema()));

    Operation operation =
        new Operation()
            .addParametersItem(firstEmbedParameter)
            .addParametersItem(secondEmbedParameter);

    OpenAPI openApi = createOpenApi(operation);

    EmbedParameterModifier modifier = new EmbedParameterModifier();

    assertThatThrownBy(() -> modifier.afterScan(null, openApi))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("The operation contains multiple 'embed' query parameters already.");
  }

  private OpenAPI createOpenApi(Operation operation) {
    Schema<?> embeddedSchema =
        new ObjectSchema().addProperty(EMBEDDABLE_PROPERTY_NAME, new ArraySchema());

    Schema<?> resourceSchema = new ObjectSchema().addProperty("_embedded", embeddedSchema);

    MediaType responseMediaType =
        new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + RESOURCE_SCHEMA_NAME));

    ApiResponse response =
        new ApiResponse().content(new Content().addMediaType(APPLICATION_JSON, responseMediaType));

    operation.setResponses(new ApiResponses().addApiResponse("200", response));

    return new OpenAPI()
        .components(new Components().addSchemas(RESOURCE_SCHEMA_NAME, resourceSchema))
        .paths(new Paths().addPathItem(TEST_PATH, new PathItem().get(operation)));
  }
}
