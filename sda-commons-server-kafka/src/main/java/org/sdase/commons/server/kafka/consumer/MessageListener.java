package org.sdase.commons.server.kafka.consumer;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.sdase.commons.server.kafka.config.ListenerConfig;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A MessageListener implements a default polling loop for retrieving messages from one to many
 * Kafka topics. It is configured by the @{@link ListenerConfig} parameters, such as the commitType,
 * the polling interval and an wait interval if the topic is not available before entering the
 * polling loop.
 *
 * <p>The listener requires a {@link KafkaConsumer} to connect to Kafka. Additionally a @{@link
 * MessageListenerStrategy} that defines how received @{@link ConsumerRecords} are handled.
 *
 * <p>If some errors occurs during record handling, the strategy might throw an {@link
 * StopListenerException} what will result in stopping the poll loop and gracefully shutdown the
 * listener.
 *
 * <p>The MessageListener does not guarantee an exactly once or at most once semantic. E.g. in case
 * of rebalancing, some messages might be received several times (eventually from different
 * consumers)
 *
 * @param <K> Class of the key of the read Kafka record
 * @param <V> Class of the value of the read Kafka record
 */
public class MessageListener<K, V> implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(MessageListener.class);
  private final long configuredPollIntervalMillis;
  private final long maxPollIntervalMillis;
  private final AtomicLong currentPollIntervalMillis;
  private final long pollIntervalFactorOnError;
  private final long topicMissingRetryMs;
  private final MessageListenerStrategy<K, V> strategy;
  private final Collection<String> topics;

  private final String joinedTopics;
  private final AtomicBoolean shouldStop = new AtomicBoolean(false);
  private final KafkaConsumer<K, V> consumer;

  public MessageListener(
      Collection<String> topics,
      KafkaConsumer<K, V> consumer,
      ListenerConfig listenerConfig,
      MessageListenerStrategy<K, V> strategy) {
    this.topics = topics;
    this.joinedTopics = String.join(",", topics);
    this.consumer = consumer;
    consumer.subscribe(topics);
    this.strategy = strategy;
    this.configuredPollIntervalMillis = listenerConfig.getPollInterval();
    this.topicMissingRetryMs = listenerConfig.getTopicMissingRetryMs();
    this.currentPollIntervalMillis = new AtomicLong(this.configuredPollIntervalMillis);
    this.pollIntervalFactorOnError = listenerConfig.getPollIntervalFactorOnError();
    this.maxPollIntervalMillis = listenerConfig.getMaxPollInterval();
    if (null != this.strategy) {
      this.strategy.setRetryCounterIfApplicable(listenerConfig.getMaxRetries());
    }
  }

  @Override
  public void run() {
    waitForTopic(joinedTopics);

    while (!shouldStop.get()) {
      // return immediately and resubmit Runnable
      try {
        ConsumerRecords<K, V> records =
            consumer.poll(Duration.ofMillis(currentPollIntervalMillis.get()));

        if (records.count() > 0) {
          LOGGER.debug("Received {} messages from topics [{}]", records.count(), joinedTopics);
        } else {
          LOGGER.trace("Received {} messages from topics [{}]", records.count(), joinedTopics);
        }

        strategy.resetOffsetsToCommitOnClose();
        strategy.processRecords(records, consumer);

        configureAfterSuccess();
      } catch (WakeupException w) {
        if (shouldStop.get()) {
          LOGGER.info("Woke up to stop consuming.");
        } else {
          LOGGER.warn("Woke up before polling returned but shouldStop is {}.", shouldStop.get(), w);
        }
      } catch (StopListenerException e) {
        LOGGER.error("Stopping listener for topics [{}] due to exception", joinedTopics, e);
        break;
      } catch (RuntimeException re) {
        LOGGER.error("Unauthorized or other runtime exception.", re);
        configureAfterError();
      }
    }
    LOGGER.info("MessageListener closing Consumer for [{}]", joinedTopics);
    try {
      strategy.commitOnClose(consumer);
    } catch (RuntimeException e) {
      LOGGER.error("Exception caught while committing offsets on close.", e);
    } finally {
      // close will auto-commit if enabled
      doCloseConsumer();
    }
  }

  private void doCloseConsumer() {
    try {
      consumer.close();
    } catch (RuntimeException e) {
      LOGGER.error("Exception caught while closing consumer.", e);
    }
  }

  private void waitForTopic(String joinedTopics) {
    // Consumer waits until the topic is up, since the KafkaConsumer.poll
    // call floods log file with warnings
    // see https://issues.apache.org/jira/browse/KAFKA-4164
    if (topicMissingRetryMs > 0) {
      while (!shouldStop.get() && !topicsReady()) {
        LOGGER.warn(
            "Topics {} are not ready yet. Waiting {} ms for retry",
            joinedTopics,
            topicMissingRetryMs);
        try {
          Thread.sleep(topicMissingRetryMs);
        } catch (InterruptedException e) {
          LOGGER.error("Thread interrupted when waiting for topic to come up");
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  boolean topicsReady() {
    return !topics.stream()
        .map(consumer::partitionsFor)
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet())
        .isEmpty();
  }

  /**
   * Method used to stop listener running in different thread. This is non blocking. You should use
   * Thread.join() (or something like that) to ensure the thread is actually shutting down.
   */
  public void stopConsumer() {
    shouldStop.set(true);

    if (consumer != null) {
      consumer.wakeup();
    }
  }

  public KafkaConsumer<K, V> getConsumer() {
    return consumer;
  }

  @Override
  public String toString() {
    return "ML ".concat(String.join("", topics));
  }

  private void configureAfterSuccess() {
    long oldValue = currentPollIntervalMillis.getAndSet(configuredPollIntervalMillis);
    if (oldValue != configuredPollIntervalMillis) {
      LOGGER.info("Resetting poll interval to {}ms after success", currentPollIntervalMillis);
    }
  }

  private void configureAfterError() {
    long nextPollIntervalAfterError = currentPollIntervalMillis.get() * pollIntervalFactorOnError;
    currentPollIntervalMillis.set(Math.min(maxPollIntervalMillis, nextPollIntervalAfterError));
    LOGGER.info("Setting poll interval to {}ms after error", currentPollIntervalMillis);
  }
}
