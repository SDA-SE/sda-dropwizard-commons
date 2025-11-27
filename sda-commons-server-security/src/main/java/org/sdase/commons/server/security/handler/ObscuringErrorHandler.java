package org.sdase.commons.server.security.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.MediaType;
import java.nio.ByteBuffer;
import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jetty.ee10.servlet.ErrorHandler;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.sdase.commons.shared.api.error.ApiError;

/**
 * Error handle that replaces default error pages of Jetty with custom {@link ApiError}.
 *
 * <p>This handler is invoked for explicitly defined error responses like {@code return
 * Response.status(404).build();} and is not invoked for response indirectly create by {@code throw
 * new NotFoundException()}.
 *
 * <p>This handler addresses risks identified in the security guide as:
 *
 * <ul>
 *   <li>"Risiko: Erkennung von vertraulichen Komponenten ... Entfernen von applikations-bezogenen
 *       Fehlermeldungen"
 * </ul>
 */
public class ObscuringErrorHandler extends ErrorHandler {

  private ObjectMapper objectMapper;

  public ObscuringErrorHandler(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public boolean handle(Request request, Response response, Callback callback) {
    response.getHeaders().add(HttpHeader.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    int status = response.getStatus();
    String bodyContent = errorBody(status);

    ByteBuffer byteBuffer = ByteBuffer.wrap(SerializationUtils.serialize(bodyContent));
    response.setStatus(status);
    response.write(true, byteBuffer, callback);
    return true;
  }

  private String errorBody(int status) {
    try {
      ApiError apiError = new ApiError();
      apiError.setTitle("HTTP Error " + status + " occurred.");
      return objectMapper.writeValueAsString(objectMapper);
    } catch (JsonProcessingException e) {
      return "{}";
    }
  }
}
