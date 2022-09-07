package org.sdase.commons.server.spring.data.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpringDataMongoConfigurationTest {

  @Test
  void shouldReturnDatabaseFromConnectionString() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration()
            .setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getDatabase()).isEqualTo("records");
  }

  @Test
  void shouldReturnUsernameFromConnectionString() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration()
            .setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getUsername()).isEqualTo("sysop");
  }

  @Test
  void shouldReturnPasswordFromConnectionString() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration()
            .setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getPassword()).isEqualTo("moon");
  }

  @Test
  void shouldReturnHostsFromConnectionString() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration()
            .setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getHosts()).isEqualTo("localhost");
  }

  @Test
  void shouldBeValidIfConnectionStringWasSet() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration()
            .setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.isValid()).isTrue();
  }

  @Test
  void shouldBeValidIfHostsAndDatabaseWasSet() {
    SpringDataMongoConfiguration config =
        new SpringDataMongoConfiguration().setHosts("localhost").setDatabase("records");
    assertThat(config.isValid()).isTrue();
  }

  @Test
  void shouldNotBeValidIfNoHostsAndDatabaseWereSet() {
    SpringDataMongoConfiguration config = new SpringDataMongoConfiguration();
    assertThat(config.isValid()).isFalse();
  }

  @Test
  void shouldNotBeValidIfOnlyHostsWasSet() {
    SpringDataMongoConfiguration config = new SpringDataMongoConfiguration().setHosts("localhost");
    assertThat(config.isValid()).isFalse();
  }

  @Test
  void shouldNotBeValidIfOnlyDatabaseWasSet() {
    SpringDataMongoConfiguration config = new SpringDataMongoConfiguration().setDatabase("records");
    assertThat(config.isValid()).isFalse();
  }
}
