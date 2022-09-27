package org.sdase.commons.server.morphia;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MongoConfigurationTest {

  @Test
  void shouldReturnDatabaseFromConnectionString() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getDatabase()).isEqualTo("records");
  }

  @Test
  void shouldReturnUsernameFromConnectionString() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getUsername()).isEqualTo("sysop");
  }

  @Test
  void shouldReturnPasswordFromConnectionString() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getPassword()).isEqualTo("moon");
  }

  @Test
  void shouldReturnHostsFromConnectionString() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getHosts()).isEqualTo("localhost");
  }

  @Test
  void shouldBeValidIfConnectionStringWasSet() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.isValid()).isTrue();
  }

  @Test
  void shouldBeValidIfHostsAndDatabaseWasSet() {
    MongoConfiguration config =
        new MongoConfiguration().setHosts("localhost").setDatabase("records");
    assertThat(config.isValid()).isTrue();
  }

  @Test
  void shouldNotBeValidIfNoHostsAndDatabaseWereSet() {
    MongoConfiguration config = new MongoConfiguration();
    assertThat(config.isValid()).isFalse();
  }

  @Test
  void shouldNotBeValidIfOnlyHostsWasSet() {
    MongoConfiguration config = new MongoConfiguration().setHosts("localhost");
    assertThat(config.isValid()).isFalse();
  }

  @Test
  void shouldNotBeValidIfOnlyDatabaseWasSet() {
    MongoConfiguration config = new MongoConfiguration().setDatabase("records");
    assertThat(config.isValid()).isFalse();
  }
}
