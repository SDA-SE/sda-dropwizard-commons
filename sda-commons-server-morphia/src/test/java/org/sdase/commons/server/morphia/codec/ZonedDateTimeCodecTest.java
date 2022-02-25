package org.sdase.commons.server.morphia.codec;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ZonedDateTimeCodecTest {

  private TimeZone defaultTimeZone;

  @Before
  public void setUp() {
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("ECT"));
  }

  @After
  public void tearDown() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  public void shouldDecodeNull() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    Object result = converter.decode(ZonedDateTime.class, null, null);

    // then
    assertThat(result).isNull();
  }

  @Test
  public void shouldDecodeDate() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Paris"));
    cal.clear();
    cal.set(2019, Calendar.MARCH, 21, 17, 22, 53);

    // when
    ZonedDateTime result =
        (ZonedDateTime) converter.decode(ZonedDateTime.class, cal.getTime(), null);

    // then
    assertThat(result).isEqualTo("2019-03-21T17:22:53+01:00[Europe/Paris]");
  }

  @Test
  public void shouldDecodeString() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    ZonedDateTime result =
        (ZonedDateTime)
            converter.decode(ZonedDateTime.class, "2019-01-21T17:22:53+01:00[Europe/Paris]", null);

    // then
    assertThat(result).isEqualTo("2019-01-21T17:22:53+01:00[Europe/Paris]");
  }

  @Test
  public void shouldEncodeNull() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    Object result = converter.encode(null, null);

    // then
    assertThat(result).isNull();
  }

  @Test
  public void shouldEncodeZonedDateTime() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    Date result =
        (Date)
            converter.encode(ZonedDateTime.parse("2019-02-21T17:22:53+01:00[Europe/Paris]"), null);

    // then
    assertThat(result).isEqualTo("2019-02-21T17:22:53.000");
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnEncodeWrongType() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    converter.encode(new Object(), null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailOnDecodeWrongType() {
    // given
    ZonedDateTimeCodec converter = new ZonedDateTimeCodec();

    // when
    converter.decode(ZonedDateTime.class, new Object(), null);
  }
}
