package org.sdase.commons.client.jersey.error;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import java.net.SocketTimeoutException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;
import org.sdase.commons.shared.api.error.ApiError;

public class ClientRequestExceptionMapperTest {

  private ClientRequestExceptionMapper clientRequestExceptionMapper =
      new ClientRequestExceptionMapper();

  @Test
  public void mapNotFound() {
    ClientRequestException exception = new ClientRequestException(new NotFoundException());

    Response response = clientRequestExceptionMapper.toResponse(exception);

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

  @Test
  public void mapInternalServerError() {
    ClientRequestException exception =
        new ClientRequestException(new InternalServerErrorException());

    Response response = clientRequestExceptionMapper.toResponse(exception);

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

  @Test
  public void mapUnknownClientError() {
    ClientRequestException exception = new ClientRequestException(new ClientErrorException(418));

    Response response = clientRequestExceptionMapper.toResponse(exception);

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

  @Test
  public void jsonProcessing() {
    ClientRequestException exception =
        new ClientRequestException(
            new ProcessingException(new JsonParseException(mock(JsonParser.class), "No message")));

    Response response = clientRequestExceptionMapper.toResponse(exception);

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

  @Test
  public void mapConnectionTimeout() {
    ClientRequestException exception =
        new ClientRequestException(new ProcessingException(new ConnectTimeoutException()));

    Response response = clientRequestExceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getHeaders().getFirst(CONTENT_TYPE))
        .isEqualTo(MediaType.APPLICATION_JSON_TYPE);
    assertThat(response.getEntity()).isInstanceOf(ApiError.class);
    assertThat((ApiError) response.getEntity())
        .extracting(ApiError::getTitle)
        .asString()
        .isNotBlank()
        .containsIgnoringCase("connect")
        .containsIgnoringCase("timeout");
  }

  @Test
  public void mapReadTimeout() {
    ClientRequestException exception =
        new ClientRequestException(new ProcessingException(new SocketTimeoutException()));

    Response response = clientRequestExceptionMapper.toResponse(exception);

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
