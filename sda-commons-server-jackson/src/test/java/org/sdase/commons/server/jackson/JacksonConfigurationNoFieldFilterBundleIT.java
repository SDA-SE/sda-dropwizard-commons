package org.sdase.commons.server.jackson;

import static io.dropwizard.testing.ConfigOverride.randomPorts;

import io.dropwizard.Configuration;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sdase.commons.server.jackson.test.JacksonConfigurationNoFieldFilterTestApp;
import org.sdase.commons.server.jackson.test.PersonResource;

class JacksonConfigurationNoFieldFilterBundleIT {

  @RegisterExtension
  public static final DropwizardAppExtension<Configuration> DW =
      new DropwizardAppExtension<>(
          JacksonConfigurationNoFieldFilterTestApp.class, null, randomPorts());

  @Test
  void shouldGetJohnDoe() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    Assertions.assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", "Johnny");
  }

  @Test
  void shouldIgnoreFieldSelection() {
    Map<String, Object> johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(new GenericType<Map<String, Object>>() {});

    Assertions.assertThat(johnny).containsKeys("_links", "firstName", "lastName", "nickName");
  }

  @Test
  void shouldNotFilterField() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "nickName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    Assertions.assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", "Johnny");
  }

  @Test
  void shouldNotFilterFieldsByMultipleParams() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "firstName")
            .queryParam("fields", "lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    Assertions.assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", "Johnny");
  }

  @Test
  void shouldNotFilterFieldsBySingleParams() {
    PersonResource johnny =
        DW.client()
            .target("http://localhost:" + DW.getLocalPort())
            .path("people")
            .path("jdoe")
            .queryParam("fields", "firstName, lastName")
            .request(MediaType.APPLICATION_JSON)
            .get(PersonResource.class);

    Assertions.assertThat(johnny)
        .extracting(
            p -> p.getSelf().getHref(),
            PersonResource::getFirstName,
            PersonResource::getLastName,
            PersonResource::getNickName)
        .containsExactly(
            "http://localhost:" + DW.getLocalPort() + "/people/jdoe", "John", "Doe", "Johnny");
  }
}
