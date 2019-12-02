package org.sdase.commons.server.kafka;

import io.dropwizard.testing.junit.DropwizardAppRule;
import org.sdase.commons.server.kafka.builder.MessageHandlerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.IgnoreAndProceedErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestApplication;
import org.sdase.commons.server.kafka.dropwizard.KafkaTestConfiguration;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;

import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sdase.commons.server.testing.DropwizardRuleHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNull;

/**
 * checks that all public bundle methods can be called without any exception
 */
public class AppDisabledKafkaServerIT {

   @ClassRule
   public static final DropwizardAppRule<KafkaTestConfiguration> DW = DropwizardRuleHelper
         .dropwizardTestAppFrom(KafkaTestApplication.class)
         .withConfigFrom(KafkaTestConfiguration::new)
         .withRandomPorts()
         .withConfigurationModifier(c -> c.getKafka().setDisabled(true))
         .build();

   private List<String> results = Collections.synchronizedList(new ArrayList<>());

   private KafkaBundle bundle;

   @Before
   public void before() {
      KafkaTestApplication app = DW.getApplication();
      bundle = app.kafkaBundle();
      results.clear();
   }

   @Test
   public void checkRegisterMessageHandler()  {
      @SuppressWarnings("unchecked")
      List<MessageListener<String, String>> lc1 = bundle
            .registerMessageHandler(MessageHandlerRegistration
                  .<String, String>builder()
                  .withListenerConfig("lc1")
                  .forTopic("topic")
                  .checkTopicConfiguration()
                  .withDefaultConsumer()
                  .withValueDeserializer(new StringDeserializer())
                  .withHandler(record -> results.add(record.value()))
                  .withErrorHandler(new IgnoreAndProceedErrorHandler<>())
                  .build());

      assertTrue(lc1.isEmpty());
   }

   @Test
   public void checkRegisterProducerReturnsDummy() {
      @SuppressWarnings("unchecked")
      MessageProducer<Object, Object> producer = bundle
            .registerProducer(
                  ProducerRegistration.builder().forTopic("Topic").createTopicIfMissing().withDefaultProducer().build()
            );
      assertNull(producer.send("test", "test"));
   }

   @Test(expected = ConfigurationException.class)
   public void checkGetTopicConfiguration()  {
      bundle.getTopicConfiguration("test");
   }





}
