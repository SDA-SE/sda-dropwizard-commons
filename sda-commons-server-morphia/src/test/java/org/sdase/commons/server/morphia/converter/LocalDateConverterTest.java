package org.sdase.commons.server.morphia.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import org.junit.Test;

public class LocalDateConverterTest {

  private LocalDateConverter localDateConverter = new LocalDateConverter();

  @Test
  public void shouldEncodeAsIsoLocalDateString() {

    LocalDate given = LocalDate.parse("2020-03-15");

    Object actual = localDateConverter.encode(given, null);

    assertThat(actual).isInstanceOf(String.class).isEqualTo("2020-03-15");
  }

  @Test
  public void shouldEncodeNullAsNull() {

    LocalDate given = null;

    Object actual = localDateConverter.encode(given, null);

    assertThat(actual).isNull();
  }

  @Test
  public void allowEncodeFailureForBadDataType() {

    ZonedDateTime given = ZonedDateTime.now();

    assertThatIllegalArgumentException().isThrownBy(() -> localDateConverter.encode(given, null));
  }

  @Test
  public void shouldDecodeIsoLocalDateString() {

    String given = "2020-03-15";

    Object actual = localDateConverter.decode(LocalDate.class, given, null);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("2020-03-15"));
  }

  @Test
  public void shouldDecodeIsoLocalDateTimeStringForBackwardCompatibility() {

    String given = "1979-02-08T00:00:00.000"; // this format has been used by Morphia for local date

    Object actual = localDateConverter.decode(LocalDate.class, given, null);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("1979-02-08"));
  }

  @Test
  public void shouldDecodeNullAsNull() {

    String given = null;

    Object actual = localDateConverter.decode(LocalDate.class, given, null);

    assertThat(actual).isNull();
  }

  @Test
  public void allowDecodeFailureForBadDataType() {

    long given = Instant.now().toEpochMilli();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> localDateConverter.decode(LocalDate.class, given, null));
  }
}
