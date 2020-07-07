package org.sdase.commons.server.weld.testing;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dropwizard.cli.Cli;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.JarLocation;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.weld.testing.test.AppConfiguration;
import org.sdase.commons.server.weld.testing.test.WeldExampleApplication;

public class WeldBundleApplicationTest {

  private static final String LOCALHOST = "http://localhost:";

  @ClassRule
  public static final DropwizardAppRule<AppConfiguration> RULE =
      new WeldAppRule<>(WeldExampleApplication.class, resourceFilePath("test-config.yaml"));

  @Test
  public void testResource() {
    String response =
        RULE.client()
            .target(LOCALHOST + RULE.getLocalPort() + "/api/dummy")
            .request()
            .get(String.class);
    assertThat(response).isEqualTo("hello foo");
  }

  @Test
  public void testServlet() {
    Response response =
        RULE.client().target(LOCALHOST + RULE.getLocalPort() + "/foo").request().get();
    assertThat(response).isNotNull();
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  public void testCommand() {
    ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
    ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
    WeldExampleApplication app = RULE.getApplication();

    final JarLocation location = mock(JarLocation.class);
    final Bootstrap<AppConfiguration> bootstrap = new Bootstrap<>(RULE.getApplication());
    when(location.toString()).thenReturn("dw-thing.jar");
    when(location.getVersion()).thenReturn(Optional.of("1.0.0"));
    bootstrap.addCommand(app.getTestCommand());

    Cli cli = new Cli(location, bootstrap, stdOut, stdErr);

    assertThat(cli.run("testDW")).isEmpty();

    assertThat(app.getTestCommand().getResult()).isEqualTo("foo");
  }

  @Test
  public void testTask() {
    WeldExampleApplication app = RULE.getApplication();

    Response response =
        RULE.client()
            .target(LOCALHOST + RULE.getAdminPort() + "/tasks/runTestTask")
            .request()
            .post(Entity.entity("", MediaType.TEXT_PLAIN));
    assertThat(response.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
    assertThat(app.getTestJobResult()).isEqualTo("foo");
  }

  @Test
  public void testEmbedTrue() {
    Boolean actual =
        RULE.client()
            .target("http://localhost:" + RULE.getLocalPort())
            .path("api")
            .path("dummy")
            .path("testLinkEmbedded")
            .queryParam("embed", "test")
            .request("application/json")
            .get(Boolean.class);

    assertThat(actual).isTrue();
  }

  @Test
  public void testEmbedFalse() {
    Boolean actual =
        RULE.client()
            .target("http://localhost:" + RULE.getLocalPort())
            .path("api")
            .path("dummy")
            .path("testLinkEmbedded")
            .request("application/json")
            .get(Boolean.class);

    assertThat(actual).isFalse();
  }
}
