package org.sdase.commons.server.jackson;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ProduceInvalidJsonOnFailureTest {

  private final ObjectMapper om = ObjectMapperConfigurationUtil.configureMapper().build();

  @Test
  void shouldNotWriteValidJsonOnSerializationError() {
    var iterator = new FailingCloseableIterator();

    var output = new ByteArrayOutputStream();

    try (var generator = om.createGenerator(output)) {
      generator.writeObject(Map.of("items", iterator));
    } catch (Exception ignored) {
      // nothing to do
    }
    var expected = new StringBuilder();
    for (int i = 1; i < 1_001; i++) {
      expected.append("\"").append(i).append("\",");
    }
    expected.deleteCharAt(expected.length() - 1);
    var actual = output.toString(UTF_8);
    // verify expected content
    assertThat(actual).isEqualToIgnoringWhitespace("{\"items\":[" + expected);
    // verify not readable
    assertThatExceptionOfType(JsonParseException.class)
        .isThrownBy(() -> om.readValue(actual, Object.class));
  }

  static class FailingCloseableIterator implements Iterator<String>, Closeable {

    final AtomicInteger i = new AtomicInteger();

    @Override
    public void close() {
      // do nothing on close
    }

    @Override
    public boolean hasNext() {
      return i.get() < 2_000;
    }

    @Override
    public String next() {
      var current = i.incrementAndGet();
      if (current > 1_000) {
        throw new IllegalStateException();
      }
      return "" + current;
    }
  }
}
