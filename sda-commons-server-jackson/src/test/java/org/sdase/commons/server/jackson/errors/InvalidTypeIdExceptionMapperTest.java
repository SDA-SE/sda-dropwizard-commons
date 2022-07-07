package org.sdase.commons.server.jackson.errors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import javax.ws.rs.core.Response;
import org.assertj.core.groups.Tuple;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.jackson.ObjectMapperConfigurationUtil;
import org.sdase.commons.server.jackson.test.ResourceWithInheritance;
import org.sdase.commons.shared.api.error.ApiError;

class InvalidTypeIdExceptionMapperTest {

  private ObjectMapper om;
  private InvalidTypeIdExceptionMapper invalidTypeIdExceptionMapper;

  @BeforeEach
  public void setUp() {
    this.om = ObjectMapperConfigurationUtil.configureMapper().build();

    // override framework default
    this.om.enable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

    this.invalidTypeIdExceptionMapper = new InvalidTypeIdExceptionMapper();
  }

  @Test
  void shouldThrowApiErrorForInvalidSubType() {
    // given
    String given = "{\"type\":\"UnknownSubType\"}";

    InvalidTypeIdException invalidTypeIdException =
        assertThrows(
            InvalidTypeIdException.class,
            () -> this.om.readValue(given, ResourceWithInheritance.class));

    // when
    Response response = this.invalidTypeIdExceptionMapper.toResponse(invalidTypeIdException);

    // then
    checkResponse(response, "");
  }

  @Test
  void shouldThrowApiErrorForInvalidSubTypeInANestedType() {
    // given
    String given =
        "{\"fieldWithNestedInheritance\":{\"fieldWithInheritance\":{\"type\":\"UnknownSubType\"}}}";

    InvalidTypeIdException invalidTypeIdException =
        assertThrows(
            InvalidTypeIdException.class,
            () -> this.om.readValue(given, ResourceWithNestedNestedInheritance.class));

    // when
    Response response = this.invalidTypeIdExceptionMapper.toResponse(invalidTypeIdException);

    // then
    checkResponse(response, "fieldWithNestedInheritance.fieldWithInheritance");
  }

  private void checkResponse(Response response, String field) {
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY_422);
    ApiError apiError = (ApiError) response.getEntity();
    assertThat(apiError).isNotNull();
    assertThat(apiError.getTitle()).isEqualTo("Invalid sub type");
    assertThat(apiError.getInvalidParams())
        .extracting("field", "reason", "errorCode")
        .containsExactlyInAnyOrder(
            Tuple.tuple(
                field,
                "Invalid sub type UnknownSubType for base type ResourceWithInheritance",
                "INVALID_SUBTYPE"));
  }

  private static class ResourceWithNestedNestedInheritance {
    private ResourceWithNestedInheritance fieldWithNestedInheritance;

    public ResourceWithNestedInheritance getFieldWithNestedInheritance() {
      return fieldWithNestedInheritance;
    }

    public ResourceWithNestedNestedInheritance setFieldWithNestedInheritance(
        ResourceWithNestedInheritance fieldWithNestedInheritance) {
      this.fieldWithNestedInheritance = fieldWithNestedInheritance;
      return this;
    }
  }

  private static class ResourceWithNestedInheritance {
    private ResourceWithInheritance fieldWithInheritance;

    public ResourceWithInheritance getFieldWithInheritance() {
      return fieldWithInheritance;
    }

    public ResourceWithNestedInheritance setFieldWithInheritance(
        ResourceWithInheritance fieldWithInheritance) {
      this.fieldWithInheritance = fieldWithInheritance;
      return this;
    }
  }
}
