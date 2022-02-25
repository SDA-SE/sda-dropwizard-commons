package org.sdase.commons.server.morphia.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.BsonWriter;
import org.junit.Test;

public class LocalDateCodecTest {

  private LocalDateCodec localDateCodec = new LocalDateCodec();

  @Test
  public void shouldEncodeAsIsoLocalDateString() {

    LocalDate given = LocalDate.parse("2020-03-15");

    BsonDocument document = new BsonDocument();
    BsonWriter writer = new BsonDocumentWriter(document);
    localDateCodec.encode(writer, given, null);

    assertThat(actual).isInstanceOf(String.class).isEqualTo("2020-03-15");
  }

  @Test
  public void shouldEncodeNullAsNull() {

    LocalDate given = null;

    Object actual = localDateCodec.encode(given, null);

    assertThat(actual).isNull();
  }

  @Test
  public void allowEncodeFailureForBadDataType() {

    ZonedDateTime given = ZonedDateTime.now();

    assertThatIllegalArgumentException().isThrownBy(() -> localDateCodec.encode(given, null));
  }

  @Test
  public void shouldDecodeIsoLocalDateString() {

    String given = "2020-03-15";

    Object actual = localDateCodec.decode(LocalDate.class, given, null);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("2020-03-15"));
  }

  @Test
  public void shouldDecodeIsoLocalDateTimeStringForBackwardCompatibility() {

    String given = "1979-02-08T00:00:00.000"; // this format has been used by Morphia for local date

    Object actual = localDateCodec.decode(LocalDate.class, given, null);

    assertThat(actual).isInstanceOf(LocalDate.class).isEqualTo(LocalDate.parse("1979-02-08"));
  }

  @Test
  public void shouldDecodeNullAsNull() {

    String given = null;

    Object actual = localDateCodec.decode(LocalDate.class, given, null);

    assertThat(actual).isNull();
  }

  @Test
  public void allowDecodeFailureForBadDataType() {

    long given = Instant.now().toEpochMilli();

    assertThatIllegalArgumentException()
        .isThrownBy(() -> localDateCodec.decode(LocalDate.class, given, null));
  }
}
