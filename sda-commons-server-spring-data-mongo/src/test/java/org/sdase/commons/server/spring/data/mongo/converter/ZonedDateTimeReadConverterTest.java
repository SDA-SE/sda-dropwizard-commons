/*
 * Copyright 2022- SDA SE Open Industry Solutions (https://www.sda.se)
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE file or at
 * https://opensource.org/licenses/MIT.
 */
package org.sdase.commons.server.spring.data.mongo.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZonedDateTimeReadConverterTest {

  private TimeZone defaultTimeZone;
  private ZonedDateTimeReadConverter converter = ZonedDateTimeReadConverter.INSTANCE;

  @BeforeEach
  void setUp() {
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("ECT"));
  }

  @AfterEach
  void tearDown() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  void shouldDecodeDate() {
    // given
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
    cal.clear();
    cal.set(2019, Calendar.MARCH, 21, 17, 22, 53);

    // when
    ZonedDateTime result = converter.convert(cal.getTime());

    // then
    assertThat(result).isEqualTo("2019-03-21T17:22:53+01:00[Europe/Paris]");
  }

  @Test
  void shouldDecodeString() {
    // when
    ZonedDateTime result = converter.convert("2019-01-21T17:22:53+01:00[Europe/Paris]");

    // then
    assertThat(result).isEqualTo("2019-01-21T17:22:53+01:00[Europe/Paris]");
  }

  @Test
  void shouldFailOnEncodeWrongType() {
    // when
    final var fromDBObject = new Object();
    assertThatThrownBy(() -> converter.convert(fromDBObject))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
