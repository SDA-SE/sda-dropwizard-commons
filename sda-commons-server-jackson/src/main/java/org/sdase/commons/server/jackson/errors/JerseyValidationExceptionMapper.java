package org.sdase.commons.server.jackson.errors;

import com.google.common.base.CaseFormat;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;

/**
 * Maps {@link JerseyViolationException}s to the error structure defined within the rest guidelines.
 * For each {@link javax.validation.ConstraintViolation}, one InvalidParam entry is generated. As
 * error code, the validation name is used as Error Code
 */
@Provider
public class JerseyValidationExceptionMapper implements ExceptionMapper<JerseyViolationException> {

  private static final String VALIDATION_EXCEPTION_MESSAGE = "Request parameters are not valid.";

  @Override
  public Response toResponse(JerseyViolationException e) {

    List<ApiInvalidParam> invalidParameters = new ArrayList<>(e.getConstraintViolations().size());

    e.getConstraintViolations()
        .forEach(
            cv -> {
              String propertyPath =
                  ConstraintMessage.isRequestEntity(cv, e.getInvocable()).orElse("N/A");
              String annotation =
                  cv.getConstraintDescriptor().getAnnotation().annotationType().toString();

              ApiInvalidParam invalidParameter =
                  new ApiInvalidParam(
                      propertyPath,
                      cv.getMessage(),
                      CaseFormat.UPPER_CAMEL.to(
                          CaseFormat.UPPER_UNDERSCORE,
                          annotation.substring(annotation.lastIndexOf('.') + 1)));
              invalidParameters.add(invalidParameter);
            });

    ApiError apiError = new ApiError(VALIDATION_EXCEPTION_MESSAGE, invalidParameters);
    return Response.status(422).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
  }
}
