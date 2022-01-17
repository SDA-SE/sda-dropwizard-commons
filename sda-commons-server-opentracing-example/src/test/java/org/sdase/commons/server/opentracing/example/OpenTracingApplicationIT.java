package org.sdase.commons.server.opentracing.example;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpenTracingApplicationIT {

  @RegisterExtension
  private static final DropwizardAppExtension<Configuration> APP =
      new DropwizardAppExtension<>(OpenTracingApplication.class, new Configuration());

  @Test
  void shouldGetInstrumented() {
    String response = webTarget("/instrumented").request().get(String.class);
    assertThat(response).isEqualTo("Done!");
  }

  @Test
  void shouldDoParam() {
    String response = webTarget("/param/foo").request().get(String.class);
    assertThat(response).isEqualTo("foo");
  }

  @Test
  void shouldGetHelloWorld() {
    String response = webTarget("/").request().get(String.class);
    assertThat(response).isEqualTo("Hello World");
  }

  @Test
  void shouldDoError() {
    Response response = webTarget("/error").request().get();
    assertThat(response.getStatus()).isEqualTo(500);
  }

  private WebTarget webTarget(String path) {
    return APP.client().target("http://localhost:" + APP.getLocalPort()).path(path);
  }
}
