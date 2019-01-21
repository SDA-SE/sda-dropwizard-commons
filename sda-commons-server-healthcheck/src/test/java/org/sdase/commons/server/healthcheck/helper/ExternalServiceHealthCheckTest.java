package org.sdase.commons.server.healthcheck.helper;

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.net.HttpURLConnection;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExternalServiceHealthCheckTest {

   private HttpURLConnection connectionMock;
   private ExternalServiceHealthCheck healthCheck;

   @Before
   public void setup() {
      connectionMock = Mockito.mock(HttpURLConnection.class);
      healthCheck = new ExternalServiceHealthCheck("http://www.testurl.com", 1000, url -> connectionMock);
   }

   @Test
   public void testExternalServiceURLOk() throws Exception {
      Mockito.when(connectionMock.getResponseCode()).thenReturn(SC_OK);
      Result result = healthCheck.check();
      assertTrue(result.isHealthy());
   }

   @Test
   public void testExternalServiceURLError() throws Exception {
      Mockito.when(connectionMock.getResponseCode()).thenReturn(SC_NOT_FOUND);
      Result result = healthCheck.check();
      assertFalse(result.isHealthy());
   }

   @Test
   public void testExternalServiceURLException() throws Exception {
      Mockito.when(connectionMock.getResponseCode()).thenThrow(new IOException());
      Result result = healthCheck.check();
      assertFalse(result.isHealthy());
   }
}
