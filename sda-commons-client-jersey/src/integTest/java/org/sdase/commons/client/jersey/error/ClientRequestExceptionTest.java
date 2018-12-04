package org.sdase.commons.client.jersey.error;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import org.apache.http.conn.ConnectTimeoutException;
import org.junit.Test;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.ProcessingException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.clientError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.connectTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.processingError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.readTimeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.serverError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.timeoutError;
import static org.sdase.commons.client.jersey.test.util.ClientRequestExceptionConditions.webApplicationExceptionCause;

public class ClientRequestExceptionTest {

   @Test
   public void identifyClientError() {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(() -> {
         throw new ClientRequestException(new NotFoundException());
      }).is(clientError()).has(webApplicationExceptionCause());
   }

   @Test
   public void identifyServerError() {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(() -> {
         throw new ClientRequestException(new InternalServerErrorException());
      }).is(serverError()).has(webApplicationExceptionCause());
   }

   @Test
   public void identifyConnectTimeout() {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(() -> {
         throw new ClientRequestException(new ProcessingException(new ConnectTimeoutException()));
      }).is(timeoutError()).is(connectTimeoutError()).doesNotHave(webApplicationExceptionCause());
   }

   @Test
   public void identifyReadTimeout() {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(() -> {
         throw new ClientRequestException(new ProcessingException(new SocketTimeoutException()));
      }).is(timeoutError()).is(readTimeoutError()).doesNotHave(webApplicationExceptionCause());
   }

   @Test
   public void identifyProcessing() {
      assertThatExceptionOfType(ClientRequestException.class).isThrownBy(() -> {
         throw new ClientRequestException(new ProcessingException(new JsonParseException(mock(JsonParser.class), "dummy")));
      }).is(processingError()).doesNotHave(webApplicationExceptionCause());
   }
}