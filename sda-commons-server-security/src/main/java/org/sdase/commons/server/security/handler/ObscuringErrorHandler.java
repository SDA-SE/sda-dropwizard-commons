package org.sdase.commons.server.security.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
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
  public void handle(
      String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    response.setContentType(MediaType.APPLICATION_JSON);
    try (PrintWriter writer = response.getWriter()) {
      int status = response.getStatus();
      String bodyContent = errorBody(status);
      writer.print(bodyContent);
    }
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
