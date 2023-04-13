package org.sdase.commons.server.spring.data.mongo;

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
  void shouldReturnNullPasswordFromConnectionString() {
    MongoConfiguration config =
        new MongoConfiguration()
            .setConnectionString("mongodb://sysop@localhost/records?authMechanism=MONGODB-X509");
    assertThat(config.getPassword()).isNull();
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
  void shouldBeValidIfConnectionStringWasSetPassword() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");
    assertThat(config.getPassword()).isEqualTo("moon");
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

  @Test
  void shouldReturnSslTrueFromConnectionStringByTlsSetting() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://localhost/db?tls=true");
    assertThat(config.isUseSsl()).isTrue();
  }

  @Test
  void shouldReturnSslFalseFromConnectionStringByTlsSetting() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://localhost/db?tls=false");
    assertThat(config.isUseSsl()).isFalse();
  }

  @Test
  void shouldReturnSslTrueFromConnectionStringBySssSetting() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://localhost/db?ssl=true");
    assertThat(config.isUseSsl()).isTrue();
  }

  @Test
  void shouldReturnSslFalseFromConnectionStringBySssSetting() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://localhost/db?ssl=false");
    assertThat(config.isUseSsl()).isFalse();
  }
}
