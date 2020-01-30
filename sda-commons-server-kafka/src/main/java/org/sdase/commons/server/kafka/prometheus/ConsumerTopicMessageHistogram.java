package org.sdase.commons.server.kafka.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Histogram;

/**
 * The central definition of the message consumer duration histogram. This is the definition of the
 * message consumed duration histogram as it should be provided by all Kafka consumers to measure
 * message process durations with Prometheus.
 */
public class ConsumerTopicMessageHistogram {

  /** The histogram name as it is published to Prometheus. */
  private static final String METRIC_NAME = "kafka_consumer_topic_message_duration";

  /** The help message description that describes the Histogram. */
  private static final String HELP = "Duration of Kafka Messages consumed in seconds.";

  /**
   * The labels added by {@code ConsumerTopicMessageHistogram}. The labels and their order have to
   * be aligned with the values created in {@link #createLabelValuesForCurrentMessage(String,
   * String)}
   */
  private static final String[] LABELS = {
    // the name of the client handling the message
    "consumer_name",
    // name of the topic records are processed from
    "topic_name"
  };

  private Histogram messageDurationHistogram;

  /**
   * Creates and registers a new {@link Histogram} matching the specification of this {@code
   * ConsumerTopicMessageHistogram} instance. <strong>Note that there should be only one registered
   * instance of this type in the application.</strong>
   */
  public ConsumerTopicMessageHistogram() {
    this.messageDurationHistogram = createAndRegister();
  }

  /** Unregisters the histogram. Should be called when the context is closed. */
  public void unregister() {
    CollectorRegistry.defaultRegistry.unregister(messageDurationHistogram);
  }

  /**
   * Observes the given message duration and adds the defined labels.
   *
   * @param durationSeconds
   * @param producerName
   * @param topicName
   */
  public void observe(double durationSeconds, String producerName, String topicName) {
    String[] labelValues = createLabelValuesForCurrentMessage(producerName, topicName);
    messageDurationHistogram.labels(labelValues).observe(durationSeconds);
  }

  /**
   * Creates all values for the labels required by the histogram in appropriate order.
   *
   * @param producerName
   * @param topicName
   * @return all values for the labels in the order they are registered in the histogram
   */
  @SuppressWarnings("static-method")
  private String[] createLabelValuesForCurrentMessage(String producerName, String topicName) {
    // the number of values and their order has to be aligned with the
    // defined #LABELS
    return new String[] {producerName, topicName};
  }

  /**
   * Builds the {@link Histogram} to measure message consumer duration and registers it.
   *
   * @return the registered {@link Histogram}
   */
  @SuppressWarnings("static-method")
  private Histogram createAndRegister() {
    Histogram.Builder histogramBuilder =
        Histogram.build().name(METRIC_NAME).labelNames(LABELS).help(HELP);
    Histogram histogram = histogramBuilder.create();
    CollectorRegistry.defaultRegistry.register(histogram);
    return histogram;
  }
}
