package org.sdase.commons.client.jersey.error;

import org.junit.Test;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ClientRequestExceptionTest {

   @Test
   public void doNotFailOnCloseIfNoResponseIsAvailable() {
      ClientRequestException clientRequestException = new ClientRequestException(new RuntimeException());
      assertThat(clientRequestException.getResponse()).isNotPresent();
      clientRequestException.close();
   }

   @Test
   public void doNotFailOnCloseException() { // NOSONAR
      Response mockResponse = mock(Response.class);
      doThrow(new RuntimeException()).when(mockResponse).close();
      when(mockResponse.getStatusInfo()).thenReturn(Response.Status.OK);

      ClientRequestException clientRequestException = new ClientRequestException(
            new WebApplicationException(mockResponse)
      );

      clientRequestException.close();
      clientRequestException.close();

      verify(mockResponse, times(2)).close();
   }

}