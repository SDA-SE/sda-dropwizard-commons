package org.sdase.commons.server.jackson.errors;

import static org.glassfish.jersey.model.Parameter.Source.QUERY;

import com.fasterxml.jackson.databind.PropertyNamingStrategies.UpperSnakeCaseStrategy;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.validation.ConstraintViolation;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.Path.Node;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.Parameter;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps {@link JerseyViolationException}s to the error structure defined within the rest guidelines.
 * For each {@link javax.validation.ConstraintViolation}, one {@link ApiInvalidParam} entry is
 * generated. As error code, the validation name is used as error code
 */
@Provider
public class JerseyValidationExceptionMapper implements ExceptionMapper<JerseyViolationException> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(JerseyValidationExceptionMapper.class);

  private static final String VALIDATION_EXCEPTION_MESSAGE = "Request parameters are not valid.";

  private static final String FORM_DATA_PARAM_ANNOTATION = "FormDataParam";
  private static final String FORM_PARAM_ANNOTATION = "FormParam";

  private static final UpperSnakeCaseStrategy ERROR_CODE_TRANSLATOR = new UpperSnakeCaseStrategy();

  @Override
  public Response toResponse(JerseyViolationException e) {

    List<ApiInvalidParam> invalidParameters = new ArrayList<>(e.getConstraintViolations().size());

    e.getConstraintViolations()
        .forEach(
            cv -> {
              String propertyPath =
                  isQueryParameterOrFormParameter(cv, e.getInvocable())
                      .orElse(
                          ConstraintMessage.isRequestEntity(cv, e.getInvocable()).orElse("N/A"));
              String annotation =
                  cv.getConstraintDescriptor().getAnnotation().annotationType().toString();

              ApiInvalidParam invalidParameter =
                  new ApiInvalidParam(
                      propertyPath,
                      cv.getMessage(),
                      camelToUpperSnakeCase(annotation.substring(annotation.lastIndexOf('.') + 1)));
              invalidParameters.add(invalidParameter);
            });

    ApiError apiError = new ApiError(VALIDATION_EXCEPTION_MESSAGE, invalidParameters);
    LOGGER.info("Validation failed. Invalid params: '{}'", apiError.getInvalidParams());
    return Response.status(422).type(MediaType.APPLICATION_JSON_TYPE).entity(apiError).build();
  }

  /**
   * Determines if constraint violation occurred in a query parameter. If it did, return a client
   * friendly string representation of the parameter where the error occurred (eg. "name").
   *
   * <p>The implementation is based on {@link ConstraintMessage#isRequestEntity}.
   */
  private static Optional<String> isQueryParameterOrFormParameter(
      ConstraintViolation<?> violation, Invocable invocable) {
    final Stream<Node> propertyPathStream =
        StreamSupport.stream(violation.getPropertyPath().spliterator(), false);
    final Path.Node parent = propertyPathStream.skip(1L).findFirst().orElse(null);
    if (parent == null) {
      return Optional.empty();
    }
    final List<Parameter> parameters = invocable.getParameters();

    if (parent.getKind() == ElementKind.PARAMETER) {
      final Parameter param =
          parameters.get(parent.as(Path.ParameterNode.class).getParameterIndex());
      final Class<? extends Annotation> annotation = param.getSourceAnnotation().annotationType();

      if (param.getSource().equals(QUERY)
          || FORM_DATA_PARAM_ANNOTATION.equals(annotation.getSimpleName())
          || FORM_PARAM_ANNOTATION.equals(annotation.getSimpleName())) {
        return Optional.of(param.getSourceName());
      }
    }

    return Optional.empty();
  }

  static String camelToUpperSnakeCase(String camelCase) {
    return ERROR_CODE_TRANSLATOR.translate(camelCase);
  }
}
