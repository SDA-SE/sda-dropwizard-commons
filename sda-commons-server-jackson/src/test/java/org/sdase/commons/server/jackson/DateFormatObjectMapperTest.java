package org.sdase.commons.server.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class DateFormatObjectMapperTest {

  private ObjectMapper om;

  private ObjectMapper alwaysMillisOm;

  private ObjectMapper onlySecondsOm;

  @BeforeEach
  void setUp() {
    this.om = ObjectMapperConfigurationUtil.configureMapper().build();
    this.alwaysMillisOm =
        ObjectMapperConfigurationUtil.configureMapper()
            .alwaysWriteZonedDateTimeWithMillis()
            .build();
    this.onlySecondsOm =
        ObjectMapperConfigurationUtil.configureMapper()
            .alwaysWriteZonedDateTimeWithoutMillis()
            .build();
  }

  @Test
  void readIso8601DateAsDate() throws Exception {
    String given = asJsonString("2018-11-21");

    Date date = om.readValue(given, Date.class);

    assertThat(date).hasYear(2018).hasMonth(11).hasDayOfMonth(21);
  }

  @Test
  void readIso8601DateAsLocalDate() throws Exception {
    String given = asJsonString("2018-11-21");

    LocalDate date = om.readValue(given, LocalDate.class);

    assertThat(date).isEqualTo("2018-11-21");
  }

  @Test
  void readIso8601DateTimeAsLocalDate() throws Exception {
    String given = asJsonString("2018-11-21T13:16:47Z");

    LocalDate date = om.readValue(given, LocalDate.class);

    assertThat(date).isEqualTo("2018-11-21");
  }

  @Test
  void readIso8601UtcTimeAsDate() throws Exception {
    String given = asJsonString("2018-11-21T13:16:47Z");

    Date date = om.readValue(given, Date.class);

    // normalize from default timezone to UTC
    // this is why Date is not good for APIs
    ZoneOffset zoneOffset =
        ZoneOffset.systemDefault().getRules().getOffset(Instant.ofEpochMilli(date.getTime()));
    date = new Date(date.getTime() - 1_000 * zoneOffset.getTotalSeconds());

    assertThat(date)
        .hasYear(2018)
        .hasMonth(11)
        .hasDayOfMonth(21)
        .hasHourOfDay(13)
        .hasMinute(16)
        .hasSecond(47);
  }

  @Test
  void readIso8601UtcTimeAsZonedDateTime() throws Exception {
    String given = asJsonString("2018-11-21T13:16:47Z");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  void readIso8601UtcTimeAsZonedDateTimeWithNamedZone() throws Exception {
    String given = asJsonString("2018-01-25T00:00Z[UTC]");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-01-25T00:00:00+00:00");
  }

  @Test
  void readIso8601CetTimeAsZonedDateTimeWithNamedZone() throws Exception {
    String given = asJsonString("2018-01-09T00:00+01:00[Europe/Paris]");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-01-09T00:00:00+01:00").isEqualTo("2018-01-08T23:00:00+00:00");
  }

  @Test
  void readIso8601UtcTimeWithMillisAsZonedDateTime() throws Exception {
    String given = asJsonString("2018-11-21T13:16:47.647Z");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47.647+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  void readIso8601ZeroOffsetWithColonTimeAsZonedDateTime() throws Exception {
    String given = asJsonString("2018-11-21T13:16:47+00:00");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  void readIso8601CetTimeAsZonedDateTime() throws Exception {
    String given = asJsonString("2018-11-21T14:16:47+01:00");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  @Disabled("https://github.com/FasterXML/jackson-modules-java8/issues/38")
  void readIso8601CetTimeAsZonedDateTimeWithoutColon() throws Exception {
    String given = asJsonString("2018-11-21T14:16:47+0100");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  void readIso8601CetTimeAsDateWithoutColon() throws Exception {
    String given = asJsonString("2018-11-21T14:16:47+0100");

    Date date = om.readValue(given, Date.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47+00:00");
  }

  @Test
  void readIso8601CetTimeAsZonedDateTimeWithNanos() throws Exception {
    String given = asJsonString("2018-11-21T14:16:47.9650003+01:00");

    ZonedDateTime date = om.readValue(given, ZonedDateTime.class);

    assertThat(date).isEqualTo("2018-11-21T13:16:47.9650003+00:00");
    assertThat(date.get(ChronoField.OFFSET_SECONDS)).isZero();
  }

  @Test
  void readIso8601Duration() throws Exception {
    String given = asJsonString("P1DT13M");

    Duration duration = om.readValue(given, Duration.class);

    assertThat(duration).isEqualTo(Duration.ofDays(1).plus(Duration.ofMinutes(13)));
  }

  @Test
  void readIso8601Period() throws Exception {
    String given = asJsonString("P1Y35D");

    Period period = om.readValue(given, Period.class);

    assertThat(period).isEqualTo(Period.ofYears(1).plus(Period.ofDays(35)));
  }

  @Test
  void writeIso8601UtcTime() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneOffset.UTC);

    String actual = om.writeValueAsString(given);

    assertThat(actual).isEqualTo(asJsonString("2018-11-21T13:16:47Z"));
  }

  @Test
  void writeIso8601CetTime() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET"));

    String actual = om.writeValueAsString(given);

    assertThat(actual).isEqualTo(asJsonString("2018-11-21T13:16:47+01:00"));
  }

  @Test
  void writeIso8601CetTimeWithMillis() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET"))
            .plus(965, ChronoUnit.MILLIS);

    String actual = om.writeValueAsString(given);

    assertThat(actual).isEqualTo(asJsonString("2018-11-21T13:16:47.965+01:00"));
  }

  @Test
  void writeIso8601CetTimeWithNanos() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneId.of("CET"));

    String actual = om.writeValueAsString(given);

    assertThat(actual).isEqualTo(asJsonString("2018-11-21T13:16:47.9650003+01:00"));
  }

  @Test
  void writeIso8601DateFromLocalDate() throws Exception {
    LocalDate date = LocalDate.of(2018, 11, 21);

    String actual = om.writeValueAsString(date);

    assertThat(actual).isEqualTo(asJsonString("2018-11-21"));
  }

  @Test
  void writeIso8601WithMillisCetIfMillisSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET"))
                    .plus(965, ChronoUnit.MILLIS));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.965+01:00"));
  }

  @Test
  void writeIso8601WithSecondsCetIfOnlyMinutesSet() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16), ZoneId.of("CET"));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:00+01:00"));
  }

  @Test
  void writeIso8601WithMillisCetIfNanosSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(
                    LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneId.of("CET")));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.965+01:00"));
  }

  @Test
  void writeIso8601UsingConfigWithMillisCetIfNanosSet() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneId.of("CET"));

    String actual = alwaysMillisOm.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.965+01:00"));
  }

  @Test
  void writeIso8601UsingConfigWithMillisCetIfSecondsSet() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET"));

    String actual = alwaysMillisOm.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.000+01:00"));
  }

  @Test
  void writeIso8601UsingConfigWithSecondsCetIfNanosSet() throws Exception {
    ZonedDateTime given =
        ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneId.of("CET"));

    String actual = onlySecondsOm.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47+01:00"));
  }

  @Test
  void writeIso8601WithMillisCetIfSecondsSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET")));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.000+01:00"));
  }

  @Test
  void writeIso8601WithMillisUtcIfMillisSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneOffset.UTC)
                    .plus(965, ChronoUnit.MILLIS));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.965Z"));
  }

  @Test
  void writeIso8601WithMillisUtcIfNanosSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(
                    LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneOffset.UTC));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.965Z"));
  }

  @Test
  void writeIso8601WithMillisUtcIfSecondsSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTimeWithMillis(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneOffset.UTC));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47.000Z"));
  }

  @Test
  void readIso8601WithNanosInPropertyFormattedWithMillis() throws Exception {
    String given =
        '{'
            + asJsonString("zonedDateTimeWithMillis")
            + ':'
            + asJsonString("2018-11-21T13:16:47.9650003Z")
            + '}';

    DateTimeHolder dateTimeHolder = om.readValue(given, DateTimeHolder.class);

    assertThat(dateTimeHolder.getZonedDateTimeWithMillis())
        .isEqualTo("2018-11-21T13:16:47.9650003Z");
  }

  @Test
  void readIso8601WithSecondsInPropertyFormattedWithMillis() throws Exception {
    String given =
        '{'
            + asJsonString("zonedDateTimeWithMillis")
            + ':'
            + asJsonString("2018-11-21T13:16:47Z")
            + '}';

    DateTimeHolder dateTimeHolder = om.readValue(given, DateTimeHolder.class);

    assertThat(dateTimeHolder.getZonedDateTimeWithMillis()).isEqualTo("2018-11-21T13:16:47Z");
  }

  @Test
  void writeIso8601WithSecondsCetIfMillisSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET"))
                    .plus(965, ChronoUnit.MILLIS));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47+01:00"));
  }

  @Test
  void writeIso8601WithSecondsCetIfNanosSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(
                    LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneId.of("CET")));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47+01:00"));
  }

  @Test
  void writeIso8601WithSecondsCetIfSecondsSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneId.of("CET")));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47+01:00"));
  }

  @Test
  void writeIso8601WithSecondsUtcIfMillisSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneOffset.UTC)
                    .plus(965, ChronoUnit.MILLIS));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47Z"));
  }

  @Test
  void writeIso8601WithSecondsUtcIfNanosSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(
                    LocalDateTime.of(2018, 11, 21, 13, 16, 47, 965_000_300), ZoneOffset.UTC));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47Z"));
  }

  @Test
  void writeIso8601WithSecondsUtcIfSecondsSet() throws Exception {
    DateTimeHolder given =
        new DateTimeHolder()
            .setZonedDateTime(
                ZonedDateTime.of(LocalDateTime.of(2018, 11, 21, 13, 16, 47), ZoneOffset.UTC));

    String actual = om.writeValueAsString(given);

    assertThat(actual).contains(asJsonString("2018-11-21T13:16:47Z"));
  }

  @Test
  void readIso8601WithNanosInPropertyFormattedWithSeconds() throws Exception {
    String given =
        '{'
            + asJsonString("zonedDateTime")
            + ':'
            + asJsonString("2018-11-21T13:16:47.9650003Z")
            + '}';

    DateTimeHolder dateTimeHolder = om.readValue(given, DateTimeHolder.class);

    assertThat(dateTimeHolder.getZonedDateTime()).isEqualTo("2018-11-21T13:16:47.9650003Z");
  }

  @Test
  void readIso8601WithSecondsInPropertyFormattedWithSeconds() throws Exception {
    String given =
        '{' + asJsonString("zonedDateTime") + ':' + asJsonString("2018-11-21T13:16:47Z") + '}';

    DateTimeHolder dateTimeHolder = om.readValue(given, DateTimeHolder.class);

    assertThat(dateTimeHolder.getZonedDateTime()).isEqualTo("2018-11-21T13:16:47Z");
  }

  private String asJsonString(String rawString) {
    return '"' + rawString + '"';
  }

  @SuppressWarnings("WeakerAccess")
  private static class DateTimeHolder {

    @JsonSerialize(using = Iso8601Serializer.class)
    private ZonedDateTime zonedDateTime;

    @JsonSerialize(using = Iso8601Serializer.WithMillis.class)
    private ZonedDateTime zonedDateTimeWithMillis;

    public ZonedDateTime getZonedDateTime() {
      return zonedDateTime;
    }

    public DateTimeHolder setZonedDateTime(ZonedDateTime zonedDateTime) {
      this.zonedDateTime = zonedDateTime;
      return this;
    }

    public ZonedDateTime getZonedDateTimeWithMillis() {
      return zonedDateTimeWithMillis;
    }

    public DateTimeHolder setZonedDateTimeWithMillis(ZonedDateTime zonedDateTimeWithMillis) {
      this.zonedDateTimeWithMillis = zonedDateTimeWithMillis;
      return this;
    }
  }
}
