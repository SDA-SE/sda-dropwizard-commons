package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.google.common.base.CaseFormat;
import io.dropwizard.jersey.validation.JerseyViolationException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.validation.ElementKind;
import javax.validation.Path;
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
  private ObjectMapper mapper = new ObjectMapper();

  @Override
  public Response toResponse(JerseyViolationException e) {

    List<ApiInvalidParam> invalidParameters = new ArrayList<>(e.getConstraintViolations().size());

    e.getConstraintViolations()
        .forEach(
            cv -> {
              String propertyPath =
                  getCorrectedPropertyPath(cv.getPropertyPath(), cv.getLeafBean());
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

  private String getCorrectedPropertyPath(Path propertyPath, Object leafBean) {
    List<String> propertyPathStrings = new ArrayList<>();
    Iterator<Path.Node> iterator = propertyPath.iterator();
    Path.MethodNode method = null;
    Path.ParameterNode parameter = null;
    Class<?> lastClazz = null;
    Path.PropertyNode lastNode = null;

    while (iterator.hasNext()) {
      Path.Node node = iterator.next();
      if (ElementKind.METHOD == node.getKind()) {
        // store method to retrieve type later
        method = (Path.MethodNode) node;
      } else if (ElementKind.PARAMETER == node.getKind()) {
        // store parameter to retrieve type later
        parameter = (Path.ParameterNode) node;
      } else if (ElementKind.PROPERTY == node.getKind()) {
        Path.PropertyNode propertyNode = (Path.PropertyNode) node;
        Class<?> includingClass = null;
        if (lastClazz == null && method != null && parameter != null) { // NOSONAR
          // not nested property
          includingClass = method.getParameterTypes().get(parameter.getParameterIndex());
          lastClazz = includingClass;
        } else if (lastClazz != null) { // NOSONAR
          // nested property
          String fieldname = lastNode.getName();
          Optional<? extends Class<?>> first =
              Stream.of(lastClazz.getDeclaredFields())
                  .filter(f -> f.getName().equalsIgnoreCase(fieldname))
                  .map(Field::getType)
                  .findFirst();
          includingClass = first.orElse(null);
        }

        // The leaf bean (i.e. the bean instance the constraint is applied on) might be a subclass
        // of includingClass. This happens when using an abstract base class and the @JsonSubTypes
        // annotation. If it matches, we use the original class instead of the abstract base class.
        if (includingClass != null && includingClass.isInstance(leafBean)) {
          includingClass = leafBean.getClass();
        }

        Optional<String> first = getPropertyName(propertyNode, includingClass);
        propertyPathStrings.add(first.orElse("N/A"));
        lastNode = propertyNode;
      }
    }
    return String.join(".", propertyPathStrings);
  }

  private Optional<String> getPropertyName(
      Path.PropertyNode propertyNode, Class<?> parameterBeanClass) {

    JavaType javaType = mapper.getTypeFactory().constructType(parameterBeanClass);
    BeanDescription introspection = mapper.getSerializationConfig().introspect(javaType);
    List<BeanPropertyDefinition> properties = introspection.findProperties();

    return properties.stream()
        .filter(property -> propertyNode.getName().equals(property.getField().getName()))
        .map(BeanPropertyDefinition::getName)
        .findFirst();
  }
}
