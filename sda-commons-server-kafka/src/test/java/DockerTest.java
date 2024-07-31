import static org.assertj.core.api.Assertions.assertThatNoException;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * This test is build for a regression introduced with an update to kafka-clients:3.8.0.
 *
 * <p>The Kafka client library introduced some kind of autodiscovery for compression configuration
 * which results in an attempt to write to the filesystem. For security reasons services built with
 * SDA Dropwizard Commons should use distroless images and a read only filesystem.
 *
 * <p>The error does not happen in regular builds. It only appears when a service bundled as docker
 * image is started with a readonly file system, e.g. in Kubernetes or with {@code docker run
 * --read-only …}. This test is used to create a {@linkplain #main(String[]) main class} for a
 * docker image (see {@code /Dockerfile} in repo root) that is executed with a read only filesystem
 * in {@code /.github/workflows/kafka-regression-test.yml} to simulate a real world scenario.
 *
 * @see <a href="https://github.com/SDA-SE/sda-dropwizard-commons/pull/3575">more details in the
 *     pull request</a>
 */
public class DockerTest {
  public static void main(String[] args) {
    System.out.println("Hello World!");
    createProducerConfig();
  }

  @Test
  void shouldCreateProducerConfig() {
    assertThatNoException().isThrownBy(DockerTest::createProducerConfig);
  }

  private static void createProducerConfig() {
    var validConfig =
        Map.<String, Object>of(
            "key.serializer",
            "org.apache.kafka.common.serialization.StringSerializer",
            "value.serializer",
            "org.apache.kafka.common.serialization.StringSerializer");
    new org.apache.kafka.clients.producer.ProducerConfig(validConfig);
  }
}
