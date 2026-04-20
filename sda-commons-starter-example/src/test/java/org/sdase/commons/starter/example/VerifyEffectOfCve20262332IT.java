package org.sdase.commons.starter.example;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sdase.commons.server.opa.testing.AbstractOpa.onRequest;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.WebTarget;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.auth.testing.AuthClassExtension;
import org.sdase.commons.server.opa.testing.OpaClassExtension;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.sdase.commons.starter.example.people.db.TestDataUtil;

class VerifyEffectOfCve20262332IT {

  @Order(0)
  @RegisterExtension
  static final OpaClassExtension OPA = new OpaClassExtension();

  @Order(1)
  @RegisterExtension
  @SuppressWarnings("unused")
  static final AuthClassExtension AUTH = AuthClassExtension.builder().build();

  @Order(2)
  @RegisterExtension
  static final DropwizardAppExtension<SdaPlatformConfiguration> DW =
      new DropwizardAppExtension<>(
          SdaPlatformExampleApplication.class,
          resourceFilePath("test-config.yaml"),
          config("opa.baseUrl", OPA::getUrl));

  @BeforeEach
  void setupTestData() {
    TestDataUtil.clearTestData();
    TestDataUtil.addPersonEntity("john-doe", "John", "Doe");
    OPA.reset();
  }

  @Test
  @SuppressWarnings("java:S3457")
  void shouldPost() throws IOException {
    OPA.mock(onRequest().withHttpMethod(HttpMethod.GET).withPath("people").deny());
    OPA.mock(onRequest().withHttpMethod(HttpMethod.POST).withPath("people").allow());
    var target = createTarget().getUri();
    System.out.println(target.getHost());
    try (Socket s = new Socket(InetAddress.getByName(target.getHost()), target.getPort())) {
      PrintWriter pw = new PrintWriter(s.getOutputStream());
      pw.print("POST /people HTTP/1.1\r\n");
      pw.print("Host: %s\r\n".formatted(target.getHost()));
      pw.print("Content-Type: application/json\r\n");
      pw.print("Content-Length: 43\r\n");
      pw.print("Connection: close\r\n");
      pw.print("\r\n");
      pw.print("{\"firstName\":\"Max\",\"lastName\":\"Mustermann\"}\r\n");
      pw.flush();
      BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        System.out.println(line);
        response.append(line);
      }
      assertThat(response).contains("Location", "people");
    }
  }

  @Test
  @SuppressWarnings("java:S3457")
  void shouldNotGet() throws IOException {
    OPA.mock(onRequest().withHttpMethod(HttpMethod.GET).withPath("people").deny());
    OPA.mock(onRequest().withHttpMethod(HttpMethod.POST).withPath("people").allow());
    var target = createTarget().getUri();
    System.out.println(target.getHost());
    try (Socket s = new Socket(InetAddress.getByName(target.getHost()), target.getPort())) {
      // adapted from https://github.com/jetty/jetty.project/security/advisories/GHSA-355h-qmc2-wpwf
      PrintWriter pw = new PrintWriter(s.getOutputStream());
      pw.print("POST /people HTTP/1.1\r\n");
      pw.print("Host: %s\r\n".formatted(target.getHost()));
      pw.print("Content-Type: application/json\r\n");
      pw.print("Transfer-Encoding: chunked\r\n");
      pw.print("\r\n");
      pw.print("1;a=\"\r\n");
      pw.print("X\r\n");
      pw.print("0\r\n");
      pw.print("\r\n");
      pw.print("GET /people HTTP/1.1\r\n");
      pw.print("Host: %s\r\n".formatted(target.getHost()));
      pw.print("Connection: close\r\n");
      pw.print("Content-Length: 11\r\n");
      pw.print("\r\n");
      pw.print("\"\r\n");
      pw.print("Y\r\n");
      pw.print("0\r\n");
      pw.print("\r\n");
      pw.flush();
      BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
      StringBuilder response = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        System.out.println(line);
        response.append(line);
      }
      assertThat(response).doesNotContain("John").contains("Not authorized");
    }
  }

  private WebTarget createTarget() {
    return DW.client().target(String.format("http://localhost:%d/", DW.getLocalPort()));
  }
}
