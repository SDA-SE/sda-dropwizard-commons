package org.sdase.commons.server.spring.data.mongo;

import static org.assertj.core.api.Assertions.assertThat;

import javax.validation.Validation;
import org.junit.jupiter.api.Test;

class MongoConfigurationTest {

  @Test
  void shouldBeValidIfConnectionStringWasSet() {
    MongoConfiguration config =
        new MongoConfiguration().setConnectionString("mongodb://sysop:moon@localhost/records");

    assertThat(Validation.buildDefaultValidatorFactory().getValidator().validate(config)).isEmpty();
  }
}
