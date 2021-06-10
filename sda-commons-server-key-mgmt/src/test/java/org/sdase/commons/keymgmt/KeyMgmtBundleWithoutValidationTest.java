package org.sdase.commons.keymgmt;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import org.eclipse.jetty.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.keymgmt.validator.PlatformKey;
import org.sdase.commons.starter.SdaPlatformBundle;

class KeyMgmtBundleWithoutValidationTest {

  @RegisterExtension
  @Order(0)
  static final DropwizardAppExtension<KeyMgmtBundleTestConfig> DW =
      new DropwizardAppExtension<>(
          KeyMgmtBundleTestApp.class,
          resourceFilePath("test-config.yml"),
          ConfigOverride.config("keyMgmt.apiKeysDefinitionPath", resourceFilePath("keys")),
          ConfigOverride.config("keyMgmt.disableValidation", "true"));

  private static WebTarget client;

  @BeforeAll
  static void init() {
    client = DW.client().target(String.format("http://localhost:%d", DW.getLocalPort()));
  }

  @Test
  void shouldValidatePlatformKeySuccess() {
    Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(Entity.entity(new ObjectWithKey().setGenderKey("MALE"), APPLICATION_JSON));
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
  }

  @Test
  void shouldAllowNoExistingValue() {
    Response response =
        client
            .path("api")
            .path("validate")
            .request()
            .post(Entity.entity(new ObjectWithKey().setGenderKey("BLA"), APPLICATION_JSON));
    assertThat(response.getStatus()).isEqualTo(HttpStatus.NO_CONTENT_204);
  }

  public static class KeyMgmtBundleTestApp extends Application<KeyMgmtBundleTestConfig> {

    private final KeyMgmtBundle<KeyMgmtBundleTestConfig> keyMgmt =
        KeyMgmtBundle.builder()
            .withKeyMgmtConfigProvider(KeyMgmtBundleTestConfig::getKeyMgmt)
            .build();

    @Override
    public void initialize(Bootstrap<KeyMgmtBundleTestConfig> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder().usingSdaPlatformConfiguration().build());
      bootstrap.addBundle(keyMgmt);
    }

    @Override
    public void run(KeyMgmtBundleTestConfig configuration, Environment environment) {
      environment.jersey().register(KeyMgmtBundleTestEndpoint.class);
    }
  }

  @Path("/")
  @Consumes(APPLICATION_JSON)
  @Produces(APPLICATION_JSON)
  public static class KeyMgmtBundleTestEndpoint {

    @POST
    @Path("/validate")
    public Response validate(@Valid ObjectWithKey valid) {
      return Response.noContent().build();
    }
  }

  public static class ObjectWithKey {
    @PlatformKey("GENDER")
    private String genderKey;

    public String getGenderKey() {
      return genderKey;
    }

    public ObjectWithKey setGenderKey(String genderKey) {
      this.genderKey = genderKey;
      return this;
    }
  }
}
