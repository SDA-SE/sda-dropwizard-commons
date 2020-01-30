package org.sdase.commons.server.jackson;

import io.dropwizard.Configuration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.assertj.core.api.Assertions;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.jackson.test.JacksonConfigurationNoFieldFilterTestApp;
import org.sdase.commons.server.jackson.test.PersonResource;

public class JacksonConfigurationNoFieldFilterBundleIT {

  @ClassRule
  public static final DropwizardAppRule<Configuration> DW =
      new DropwizardAppRule<>(
          JacksonConfigurationNoFieldFilterTestApp.class,
          ResourceHelpers.resourceFilePath("test-config.yaml"));

  @Test
  public void shouldGetJohnDoe() {
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
  public void shouldIgnoreFieldSelection() {
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
  public void shouldNotFilterField() {
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
  public void shouldNotFilterFieldsByMultipleParams() {
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
  public void shouldNotFilterFieldsBySingleParams() {
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
