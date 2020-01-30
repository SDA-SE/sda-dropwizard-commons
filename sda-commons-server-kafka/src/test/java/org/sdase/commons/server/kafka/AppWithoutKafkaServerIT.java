package org.sdase.commons.server.kafka;

import com.google.common.collect.Lists;
import io.dropwizard.testing.junit.DropwizardAppRule;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

public class AppWithoutKafkaServerIT {

  @ClassRule
  public static final DropwizardAppRule<KafkaTestConfiguration> DW =
      DropwizardRuleHelper.dropwizardTestAppFrom(KafkaTestApplication.class)
          .withConfigFrom(KafkaTestConfiguration::new)
          .withRandomPorts()
          .withConfigurationModifier(
              c -> c.getKafka().setBrokers(Lists.newArrayList("PLAINTEXT://127.0.0.1:1")))
          .build();

  private List<String> results = Collections.synchronizedList(new ArrayList<>());

  private KafkaBundle<KafkaTestConfiguration> bundle;

  @Before
  public void before() {
    KafkaTestApplication app = DW.getApplication();
    //noinspection unchecked
    bundle = app.kafkaBundle();
    results.clear();
  }

  @Test(expected = TimeoutException.class)
  public void checkMessageListenerCreationThrowsException() {
    String topicName = "checkMessageListenerCreationSuccessful";
    bundle.registerMessageHandler(
        MessageHandlerRegistration.<String, String>builder()
            .withListenerConfig("lc1")
            .forTopic(topicName)
            .checkTopicConfiguration()
            .withDefaultConsumer()
            .withValueDeserializer(new StringDeserializer())
            .withHandler(record -> results.add(record.value()))
            .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
            .build());
  }

  @Test(expected = TimeoutException.class)
  public void checkProducerWithCreationThrowsException() {
    String topicName = "checkProducerWithCreationThrowsException";
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topicName)
            .createTopicIfMissing()
            .withDefaultProducer()
            .build());
  }

  @Test(expected = TimeoutException.class)
  public void checkProducerWithCheckThrowsException() {
    String topicName = "checkProducerWithCreationThrowsException";
    bundle.registerProducer(
        ProducerRegistration.builder()
            .forTopic(topicName)
            .checkTopicConfiguration()
            .withDefaultProducer()
            .build());
  }
}
