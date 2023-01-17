package org.sdase.commons.server.spring.data.mongo.converter.morphia.compatibility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LocalDateConverterTest {

  private final LocalDateWriteConverter encoder = LocalDateWriteConverter.INSTANCE;
  private final LocalDateReadConverter decoder = LocalDateReadConverter.INSTANCE;

  @Test
  void shouldEncodeAsIsoLocalDateString() {

    LocalDate given = LocalDate.parse("2020-03-15");

    Object actual = encoder.convert(given);

    assertThat(actual).isInstanceOf(String.class).isEqualTo("2020-03-15");
  }

  @Test
  void shouldDecodeIsoLocalDateString() {

    String given = "2020-03-15";

    Object actual = decoder.convert(given);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("2020-03-15"));
  }

  @Test
  void shouldDecodeIsoLocalDateTimeStringForBackwardCompatibility() {

    String given = "1979-02-08T00:00:00.000"; // this format has been used by Morphia for local date

    Object actual = decoder.convert(given);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("1979-02-08"));
  }
}
