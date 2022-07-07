package org.sdase.commons.server.jackson.errors;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.eclipse.jetty.http.HttpStatus;
import org.sdase.commons.shared.api.error.ApiError;
import org.sdase.commons.shared.api.error.ApiInvalidParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
public class InvalidTypeIdExceptionMapper implements ExceptionMapper<InvalidTypeIdException> {

  private static final Logger LOGGER = LoggerFactory.getLogger(InvalidTypeIdExceptionMapper.class);

  @Override
  public Response toResponse(InvalidTypeIdException exception) {
    LOGGER.error("Invalid sub type", exception);

    String field =
        exception.getPath().stream()
            .map(JsonMappingException.Reference::getFieldName)
            .filter(Objects::nonNull)
            .collect(Collectors.joining("."));

    List<ApiInvalidParam> invalidParams = new ArrayList<>();
    invalidParams.add(
        new ApiInvalidParam(
            field,
            MessageFormat.format(
                "Invalid sub type {0} for base type {1}",
                exception.getTypeId(), exception.getBaseType().getRawClass().getSimpleName()),
            "INVALID_SUBTYPE"));

    ApiError apiError = new ApiError("Invalid sub type", invalidParams);
    return Response.status(HttpStatus.UNPROCESSABLE_ENTITY_422)
        .type(MediaType.APPLICATION_JSON)
        .entity(apiError)
        .build();
  }
}
