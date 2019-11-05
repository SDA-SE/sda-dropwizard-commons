package org.sdase.commons.server.auth.key;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.junit.Test;

public class JwksKeySourceTest {

   @Test()
   public void shouldCloseResponseOnException() {
      Response response = mock(Response.class);
      Client client = mock(Client.class);
      WebApplicationException webApplicationException = mock(WebApplicationException.class);
      doReturn(response).when(webApplicationException).getResponse();
      doThrow(webApplicationException).when(client).target(anyString());
      doThrow(new ProcessingException("Test")).when(response).close();
      JwksKeySource jwksKeySource = new JwksKeySource("uri", client);

      assertThatExceptionOfType(KeyLoadFailedException.class).isThrownBy(jwksKeySource::loadKeysFromSource);

      verify(response).close();
   }

}