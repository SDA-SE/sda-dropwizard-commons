package org.sdase.commons.server.kafka.consumer.strategies.deadletter.dead;

import com.google.common.collect.ImmutableMultimap;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.KafkaClientManager;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The dead letter retry task is used to re-insert messages that end up in the dead letter topic
 * into the original topic again. The task is triggered from external via HTTP
 */
public class DeadLetterTriggerTask extends Task {

  private final KafkaClientManager kafkaClientManager;

  private static final Logger LOG = LoggerFactory.getLogger(DeadLetterTriggerTask.class);

  public DeadLetterTriggerTask(KafkaClientManager kafkaClientManager) {
    super("deadLetterResend/" + kafkaClientManager.getDeadLetterTopicName());
    this.kafkaClientManager = kafkaClientManager;
  }

  @Override
  public void execute(ImmutableMultimap<String, String> parameters, PrintWriter printWriter) {

    boolean continueReading = true;
    AtomicInteger reinserted = new AtomicInteger(0);

    try (KafkaConsumer<byte[], byte[]> consumer =
        kafkaClientManager.createConsumerForDeadLetterTask()) {
      try (MessageProducer<byte[], byte[]> sourceProducer =
          kafkaClientManager.createDeadLetterToMainTopicProducer()) {
        consumer.subscribe(Collections.singletonList(kafkaClientManager.getDeadLetterTopicName()));

        while (continueReading) {
          final ConsumerRecords<byte[], byte[]> records = consumer.poll(100);

          if (records.isEmpty()) {
            continueReading = false;
          }

          if (LOG.isInfoEnabled()) {
            LOG.info(
                "Read {} records  for main topic insertion. Consumer Status before commit: {} ",
                records.count(),
                consumer.assignment().stream()
                    .map(
                        tp ->
                            String.format(
                                "[Topic: %s, Partition:%s, Offset: %s]",
                                tp.topic(),
                                tp.partition(),
                                consumer.committed(tp) != null
                                    ? consumer.committed(tp).offset()
                                    : "unknown"))
                    .collect(Collectors.joining(",")));
          }
          // notice: header will be removed since retry information is not interesting any longer
          records.forEach(
              record -> {
                sourceProducer.send(record.key(), record.value());
                reinserted.getAndIncrement();
              });
        }
        printWriter.println(String.format("reinserted %s messages", reinserted.get()));
      }
    }
  }
}
