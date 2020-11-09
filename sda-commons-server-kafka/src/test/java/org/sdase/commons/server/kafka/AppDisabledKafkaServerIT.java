package org.sdase.commons.server.kafka;

import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;

/** checks that all public bundle methods can be called without any exception */
public class AppDisabledKafkaServerIT {

  @ClassRule
  public static final DropwizardAppRule<KafkaTestConfiguration> DW =
      new DropwizardAppRule<>(
          KafkaTestApplication.class,
          resourceFilePath("test-config-default.yml"),
          config("kafka.disabled", "true"));

  private List<String> results = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> bundle;

  @Before
  public void before() {
    KafkaTestApplication app = DW.getApplication();
    bundle = app.kafkaBundle();
    results.clear();
  }

  @Test
  public void checkRegisterMessageHandler() {
    List<MessageListener<Object, String>> lc1 =
        bundle.createMessageListener(
            MessageListenerRegistration.builder()
                .withListenerConfig("lc1")
                .forTopic("topic")
                .checkTopicConfiguration()
                .withDefaultConsumer()
                .withValueDeserializer(new StringDeserializer())
                .withListenerStrategy(
                    new SyncCommitMLS<>(
                        record -> results.add(record.value()),
                        new IgnoreAndProceedErrorHandler<>()))
                .build());

    assertTrue(lc1.isEmpty());
  }

  @Test
  public void checkRegisterProducerReturnsDummy() {
    MessageProducer<Object, Object> producer =
        bundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic("Topic")
                .createTopicIfMissing()
                .withDefaultProducer()
                .build());
    assertNull(producer.send("test", "test"));
  }

  @Test(expected = ConfigurationException.class)
  public void checkGetTopicConfiguration() {
    bundle.getTopicConfiguration("test");
  }
}
