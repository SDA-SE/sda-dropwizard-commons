package org.sdase.commons.server.kafka.prometheus;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;

/**
 * The central definition of the response duration histogram. This is the
 * definition of the response duration histogram as it should be provided by all
 * SDA services to measure message durations with Prometheus.
 */
public class ProducerTopicMessageCounter {

   /**
    * The counter name as it is published to Prometheus.
    */
   private static final String METRIC_NAME = "kafka_producer_topic_message_total";

   /**
    * The help message description that describes the Histogram.
    */
   private static final String HELP = "Amount of messages published by Kafka producers.";

   /**
    * The labels added by {@code RequestDurationHistogram}. The labels and their
    * order have to be aligned with the values created in
    * {@link #createLabelValuesForCurrentRequest(String, String)}
    */
   private static final String[] LABELS = {
         // the name of the client handling the message
         "producer_name",
         // name of the topic records are processed from
         "topic_name" };

   private Counter topicMessagesCounter;

   /**
    * Creates and registers a new {@link Counter} matching the specification of
    * this {@code TopicCounterSpecification} instance. <strong>Note that there
    * should be only one registered instance of this type in the
    * application.</strong>
    */
   public ProducerTopicMessageCounter() {
      this.topicMessagesCounter = createAndRegister();
   }

   /**
    * Unregisters the histogram. Should be called when the context is closed.
    */
   public void unregister() {
      CollectorRegistry.defaultRegistry.unregister(topicMessagesCounter);
   }

   /**
    * Increases the counter by 1.
    * 
    * @see io.prometheus.client.Counter.Child#inc
    */
   public void increase(String producerName, String topicName) {
      String[] labelValues = createLabelValuesForCurrentRequest(producerName, topicName);
      topicMessagesCounter.labels(labelValues).inc();
   }

   /**
    * Creates all values for the labels required by the counter in appropriate
    * order.
    * 
    * @param producerName producer name
    * @param topicName topic name
    * @return array of labels
    */
   @SuppressWarnings("static-method")
   private String[] createLabelValuesForCurrentRequest(String producerName, String topicName) {
      // the number of values and their order has to be aligned with the
      // defined #LABELS
      return new String[] { producerName, topicName };
   }

   /**
    * Builds the {@link Counter} to measure the number of messages.
    *
    * @return the registered {@link Counter}
    */
   @SuppressWarnings("static-method")
   private Counter createAndRegister() {
      Counter.Builder topicMessageCounterBuilder = Counter.build().name(METRIC_NAME).labelNames(LABELS).help(HELP);
      Counter counter = topicMessageCounterBuilder.create();
      CollectorRegistry.defaultRegistry.register(counter);
      return counter;
   }
}
