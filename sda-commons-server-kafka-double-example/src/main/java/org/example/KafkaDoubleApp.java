package org.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.sdase.commons.server.kafka.KafkaBundle;
import org.sdase.commons.server.kafka.KafkaConfiguration;
import org.sdase.commons.server.kafka.builder.MessageListenerRegistration;
import org.sdase.commons.server.kafka.builder.ProducerRegistration;
import org.sdase.commons.server.kafka.consumer.strategies.synccommit.SyncCommitMLS;
import org.sdase.commons.server.kafka.serializers.KafkaJsonDeserializer;
import org.sdase.commons.server.kafka.serializers.KafkaJsonSerializer;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An example to show how to connect to two different Kafka brokers. */
public class KafkaDoubleApp extends Application<KafkaDoubleApp.Config> {

  private static final Logger LOG = LoggerFactory.getLogger(KafkaDoubleApp.class);

  private final KafkaBundle<Config> sourceReaderBundle =
      KafkaBundle.builder()
          .withConfigurationProvider(Config::getKafkaSource)
          .withHealthCheckName("kafkaSourceBroker")
          .build();
  private final KafkaBundle<Config> targetWriterBundle =
      KafkaBundle.builder()
          .withConfigurationProvider(Config::getKafkaTarget)
          .withHealthCheckName("kafkaTargetBroker")
          .build();

  public static void main(String[] args) throws Exception {
    new KafkaDoubleApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<Config> bootstrap) {
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build());
    bootstrap.addBundle(sourceReaderBundle);
    bootstrap.addBundle(targetWriterBundle);
  }

  @Override
  public void run(Config configuration, Environment environment) throws JsonProcessingException {

    String configJson =
        environment
            .getObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(configuration);
    LOG.info("Config:\n{}", configJson);

    var targetProducer =
        targetWriterBundle.registerProducer(
            ProducerRegistration.builder()
                .forTopic(configuration.getKafkaTarget().getTopics().get("target-topic").getName())
                .withDefaultProducer()
                .withKeySerializer(new StringSerializer())
                .withValueSerializer(new KafkaJsonSerializer<>(environment.getObjectMapper()))
                .build());

    sourceReaderBundle.createMessageListener(
        MessageListenerRegistration.builder()
            .withDefaultListenerConfig()
            .forTopic(configuration.getKafkaSource().getTopics().get("source-topic").getName())
            .withDefaultConsumer()
            .withKeyDeserializer(new StringDeserializer())
            .withValueDeserializer(
                new KafkaJsonDeserializer<>(environment.getObjectMapper(), Message.class))
            .withListenerStrategy(
                new SyncCommitMLS<>(
                    cr -> {
                      try {
                        var forward = new Message(cr.value().number() * 2);
                        targetProducer.send(cr.key(), forward).get();
                      } catch (InterruptedException e) {
                        LOG.warn("Failed to send {}", cr.key());
                        Thread.currentThread().interrupt();
                      } catch (ExecutionException e) {
                        throw new IllegalStateException(e);
                      }
                    },
                    (cr, e, c) -> true))
            .build());
  }

  public static class Config extends SdaPlatformConfiguration {

    private KafkaConfiguration kafkaSource = new KafkaConfiguration();
    private KafkaConfiguration kafkaTarget = new KafkaConfiguration();

    public KafkaConfiguration getKafkaSource() {
      return kafkaSource;
    }

    public Config setKafkaSource(KafkaConfiguration kafkaSource) {
      this.kafkaSource = kafkaSource;
      return this;
    }

    public KafkaConfiguration getKafkaTarget() {
      return kafkaTarget;
    }

    public Config setKafkaTarget(KafkaConfiguration kafkaTarget) {
      this.kafkaTarget = kafkaTarget;
      return this;
    }
  }

  record Message(Integer number) {}
}
