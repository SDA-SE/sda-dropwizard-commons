package org.sdase.commons.client.jersey.error;

import static org.apache.hc.core5.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.SocketTimeoutException;
import org.apache.hc.client5.http.ConnectTimeoutException;
import org.junit.jupiter.api.Test;
import org.sdase.commons.shared.api.error.ApiError;

class ClientRequestExceptionMapperTest {

  private ClientRequestExceptionMapper clientRequestExceptionMapper =
      new ClientRequestExceptionMapper();

  @Test
  void mapNotFound() {
    ClientRequestException exception = new ClientRequestException(new NotFoundException());

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .contains("404");
    }
  }

  @Test
  void mapInternalServerError() {
    ClientRequestException exception =
        new ClientRequestException(new InternalServerErrorException());

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .contains("500");
    }
  }

  @Test
  void mapUnknownClientError() {
    ClientRequestException exception = new ClientRequestException(new ClientErrorException(418));

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .contains("418");
    }
  }

  @Test
  void jsonProcessing() {
    ClientRequestException exception =
        new ClientRequestException(
            new ProcessingException(new JsonParseException(mock(JsonParser.class), "No message")));

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .containsIgnoringCase("processing");
    }
  }

  @Test
  void mapConnectionTimeout() {
    ClientRequestException exception =
        new ClientRequestException(
            new ProcessingException(
                new ConnectTimeoutException(ConnectTimeoutException.class.descriptorString())));

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .containsIgnoringCase("Connect")
          .containsIgnoringCase("timeout");
    }
  }

  @Test
  void mapReadTimeout() {
    ClientRequestException exception =
        new ClientRequestException(new ProcessingException(new SocketTimeoutException()));

    try (Response response = clientRequestExceptionMapper.toResponse(exception)) {

      assertThat(response.getStatus()).isEqualTo(500);
      assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
          .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
      assertThat(response.getEntity()).isInstanceOf(ApiError.class);
      assertThat((ApiError) response.getEntity())
          .extracting(ApiError::getTitle)
          .asString()
          .isNotBlank()
          .containsIgnoringCase("read")
          .containsIgnoringCase("timeout");
    }
  }
}
