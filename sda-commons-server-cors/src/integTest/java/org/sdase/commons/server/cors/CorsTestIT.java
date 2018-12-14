package org.sdase.commons.server.cors;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.cors.test.CorsTestConfiguration;
import org.sdase.commons.server.cors.test.CorsTestApp;
import org.sdase.commons.shared.tracing.ConsumerTracing;
import org.sdase.commons.shared.tracing.RequestTracing;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CorsTestIT {

   @ClassRule
   public static DropwizardAppRule<CorsTestConfiguration> DW_ALLOW = new DropwizardAppRule<>(CorsTestApp.class,
         ResourceHelpers.resourceFilePath("test-config.yaml"));

   @ClassRule
   public static DropwizardAppRule<CorsTestConfiguration> DW_DENY = new DropwizardAppRule<>(CorsTestApp.class,
         ResourceHelpers.resourceFilePath("test-config-deny.yaml"));

   @ClassRule
   public static DropwizardAppRule<CorsTestConfiguration> DW_RESTRICTED = new DropwizardAppRule<>(CorsTestApp.class,
         ResourceHelpers.resourceFilePath("test-config-restricted.yaml"));

   private String allowAllEndpoint = "http://localhost:" + DW_ALLOW.getLocalPort() + "/samples/empty";
   private String denyEndpoint = "http://localhost:" + DW_DENY.getLocalPort() + "/samples/empty";
   private String restrictedEndpoint = "http://localhost:" + DW_RESTRICTED.getLocalPort() + "/samples/empty";
   private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS,HEAD,PATCH";

   @BeforeClass
   public static void setup() {
      // allow to set headers in jersy client
      System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
   }

   @Test
   public void shouldNotSetHeaderWhenDeny() {
      Response response = DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", "server-a.com")
            .get();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
   }

   @Test
   public void shouldSetHeaderWhenAllow() {
      String origin = "some.com";
      Response response = DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
            .isEqualTo("Location");
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
            .isEqualTo(Boolean.TRUE.toString());
   }

   @Test
   public void shouldSetHeaderWhenOriginAllowed() {
      String origin = "server-a.com";
      Response response = DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_EXPOSE_HEADERS_HEADER))
            .isEqualTo("Location");
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
            .isEqualTo(Boolean.TRUE.toString());
   }

   @Test
   public void shouldNotSetHeaderWhenOriginNotAllowed() {
      String origin = "server-b.com";
      Response response = DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .get();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
   }

   @Test
   public void shouldNotSetHeaderWhenAllowedPreflight() {
      String origin = "server-a.com";
      Response response = DW_DENY
            .client()
            .target(denyEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER, "POST")
            .options();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
   }

   @Test
   public void shouldSetHeaderWhenAllowPreflight() {
      String origin = "some.com";
      Response response = DW_ALLOW
            .client()
            .target(allowAllEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
            .isEqualTo(allowedMethods);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
            .isEqualTo(Boolean.TRUE.toString());
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
            .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
   }

   @Test
   public void shouldSetHeaderWhenOriginAllowedPreflight() {
      String origin = "server-a.com";
      Response response = DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isEqualTo(origin);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER))
            .isEqualTo(allowedMethods);
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER))
            .isEqualTo(Boolean.TRUE.toString());
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER).split(","))
            .containsExactlyInAnyOrder(getAllowedHeaderList("some"));
   }

   @Test
   public void shouldNotSetHeaderWhenOriginNotAllowedPreflight() {
      String origin = "server-b.com";
      Response response = DW_RESTRICTED
            .client()
            .target(restrictedEndpoint)
            .request(MediaType.APPLICATION_JSON)
            .header("Origin", origin)
            .header(CrossOriginFilter.ACCESS_CONTROL_REQUEST_METHOD_HEADER, "POST")
            .options();

      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_ORIGIN_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_HEADERS_HEADER)).isNullOrEmpty();
      assertThat(response.getHeaderString(CrossOriginFilter.ACCESS_CONTROL_ALLOW_METHODS_HEADER)).isNullOrEmpty();
   }

   private String[] getAllowedHeaderList(String... configured) {
      List<String> allowedHeaders = new ArrayList<>(Arrays.asList(configured));
      allowedHeaders.add("Content-Type");
      allowedHeaders.add("Authorization");
      allowedHeaders.add("X-Requested-With");
      allowedHeaders.add("Accept");
      allowedHeaders.add(ConsumerTracing.TOKEN_HEADER);
      allowedHeaders.add(RequestTracing.TOKEN_HEADER);
      return allowedHeaders.toArray(new String[0]);
   }

}
