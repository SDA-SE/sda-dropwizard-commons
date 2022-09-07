package org.sdase.commons.server.spring.data.mongo.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ZonedDateTimeConverterTest {

  private TimeZone defaultTimeZone;

  @BeforeEach
  void setUp() {
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("ECT"));
  }

  @AfterEach
  public void tearDown() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  void shouldDecodeDate() {
    // given
    DateToZonedDateTimeConverter converter = new DateToZonedDateTimeConverter();
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
    // given
    StringToZonedDateTimeConverter converter = new StringToZonedDateTimeConverter();

    // when
    ZonedDateTime result = converter.convert("2019-01-21T17:22:53+01:00[Europe/Paris]");

    // then
    assertThat(result).isEqualTo("2019-01-21T17:22:53+01:00[Europe/Paris]");
  }

  @Test
  void shouldEncodeZonedDateTime() {
    // given
    ZonedDateTimeToDateConverter converter = new ZonedDateTimeToDateConverter();

    // when
    Date result = converter.convert(ZonedDateTime.parse("2019-02-21T17:22:53+01:00[Europe/Paris]"));

    // then
    assertThat(result).isEqualTo("2019-02-21T17:22:53.000");
  }
}
