package org.sdase.commons.server.kafka.consumer.strategies.deadletter;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;

import io.dropwizard.setup.Environment;
import io.prometheus.client.SimpleTimer;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.CommitFailedException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.KafkaHelper;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.consumer.MessageListener;
import org.sdase.commons.server.kafka.consumer.StopListenerException;
import org.sdase.commons.server.kafka.consumer.strategies.MessageListenerStrategy;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.dead.DeadLetterTriggerTask;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.helper.DeserializerResult;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.helper.NoSerializationErrorDeserializer;
import org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry.RetryHandler;
import org.sdase.commons.server.kafka.exception.ConfigurationException;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link MessageListenerStrategy Strategy} that stores messages with processing errors to a
 * separate topic. It allows a configurable number of retries for a failed message until it will be
 * pushed to a third topic (dead letter topic).
 *
 * Messages can be manually copied from the dead letter topic back to the original topic using the
 * {@link DeadLetterTriggerTask dead letter admin task} which will be automatically registered in
 * your application.
 *
 * To use the strategy the {@link NoSerializationErrorDeserializer} needs to be used as a wrapper
 * for key and value deserializer
 */
public class DeadLetterMLS<K extends Serializable, V extends Serializable> extends
    MessageListenerStrategy<DeserializerResult<K>, DeserializerResult<V>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DeadLetterMLS.class);
  private static final String EXCEPTION = "Exception";
  private static final String RETRIES = "Retries";

  private final MessageHandler<K, V> handler;
  private final ErrorHandler<K, V> errorHandler;

  // producer writing in retry topic
  private final MessageProducer<byte[], byte[]> retryProducer;
  // produder writing in dead letter topic
  private final MessageProducer<byte[], byte[]> deadLetterTopicProducer;

  private final int maxNumberOfRetries;
  private String consumerName;
  private List<MessageListener<byte [], byte []>> retryListener;

  /**
   * @param environment environment of the dropwizard app that will be used to create an admin task
   * @param handler handler that processes the message and dead letter topic.
   * @param maxNumberOfRetries number of automated retries before the message will end in dead
   * letter queue
   */
  public DeadLetterMLS(Environment environment, MessageHandler<K, V> handler,
      KafkaClientManager kafkaClientManager, int maxNumberOfRetries,
      int retryIntervalMS) {
    this(environment, handler, null, kafkaClientManager,
        maxNumberOfRetries, retryIntervalMS);
  }

  /**
   * @param environment environment of the dropwizard app that will be used to create an admin task
   * @param handler handler that processes the message
   * @param errorHandler error handler that will be used if the handler throws an exception and dead
   * letter topic.
   * @param maxNumberOfRetries number of automated retries before the message will end in dead
   * letter queue
   */
  public DeadLetterMLS(Environment environment,  //NOSONAR
      MessageHandler<K, V> handler,
      ErrorHandler<K, V> errorHandler,
      KafkaClientManager kafkaClientManager,
      int maxNumberOfRetries,
      int retryIntervalMS) {

    this.handler = handler;
    this.errorHandler = errorHandler;

    this.retryProducer = kafkaClientManager.createMainToRetryTopicProducer();
    this.deadLetterTopicProducer = kafkaClientManager.createDeadLetterProducer();

    this.maxNumberOfRetries = maxNumberOfRetries;

    // Init the retry mechanism
    final RetryHandler retryHandler = new RetryHandler(
        kafkaClientManager.createRetryToMainTopicProducer(), retryIntervalMS);
    this.retryListener = kafkaClientManager.createConsumerForRetryTopic(retryHandler);

    // init the task to copy messages from dead letter topic back to source topic
    environment
        .admin()
        .addTask(new DeadLetterTriggerTask(kafkaClientManager));
  }


  @Override
  public void processRecords( // NOSONAR
      ConsumerRecords<DeserializerResult<K>, DeserializerResult<V>> records,
      KafkaConsumer<DeserializerResult<K>, DeserializerResult<V>> consumer) {
    if (consumerName == null) {
      consumerName = KafkaHelper.getClientId(consumer);
    }

    for (ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record : records) {
      ConsumerRecord<K, V> kvRecord = createKvRecord(record);
      try {
        handleRecord(record, kvRecord);
      } catch (RuntimeException e) {
        // something goes wrong during serialization or in business logic. Try compensation with error handler
        boolean shouldContinue = true;
        RuntimeException deadLetterCause = null;
        if (errorHandler != null && isDeserializedCorrectly(record)) {
          try {
            shouldContinue = handleErrorWithErrorHandler(kvRecord, e);
          } catch (RuntimeException errorHandlerException) {
            // if no error handler set or default error handing throws an exception
            // the message is processed in dead letter handling
            deadLetterCause = errorHandlerException;
          }
        } else {
          // if no error handler is set, directly start dead letter processing
          deadLetterCause = e;
        }
        if (!shouldContinue) {
          throw new StopListenerException(new RuntimeException(  //NOSONAR
              "Listener Stopped because error handler returned false"));
        }
        if (deadLetterCause != null) {
          deadLetterHandling(
              (record.key() == null)? null: record.key().getByteValue(),
              (record.value() == null)? null: record.value().getByteValue(),
              record.headers(),
              deadLetterCause
          );
        }
      }
    }
    consumer.commitSync();
  }

  private void handleRecord(ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record,
      ConsumerRecord<K, V> kvRecord) {
    if (isDeserializedCorrectly(record)) {
      // record is ok, key and value has been deserialized successfully.
      processRecord(kvRecord);
    } else {
      // record is not ok, either key or value has no valid value due to deserialization issues
      rethrowDeserializationException(record);
    }
  }

  /**
   * Processing of a single record.
   * <p>
   * If the record has serialization exceptions and therewith no valid values, the serialization
   * exception is thrown again and therewith the message is pushed to exception handling.
   * </p>
   *
   * @param record the record to be processed
   */
  private void processRecord(ConsumerRecord<K, V> record) {

    final SimpleTimer timer = new SimpleTimer();

    // business processing of the record. A new record is created that hides the
    // structure used to transfer serialization exceptions
    handler.handle(record);

    // Prometheus
    final double elapsedSeconds = timer.elapsedSeconds();
    consumerProcessedMsgHistogram.observe(elapsedSeconds, consumerName, record.topic());

    if (LOGGER.isTraceEnabled()) {
      LOGGER
          .trace("calculated duration {} for message consumed by {} from {}", elapsedSeconds,
              consumerName, record.topic());
    }
  }


  /**
   * @param record the record to check
   * @return true if deserialization of the record was successful
   */
  private boolean isDeserializedCorrectly(
      ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record) {
    return (record.key() == null || record.key().parsedSuccessful()) && (record.value() == null || record.value().parsedSuccessful());
  }

  /**
   * Throws the deserialization exception, that has been caught during deserialization before
   */
  private void rethrowDeserializationException(
      ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record) {
    if (record.key() != null && record.key().hasError()) {
      throw record.key().getError();
    } else if (record.value() != null && record.value().hasError()){
      throw record.value().getError();
    }
    LOGGER.error("Method should only be invoked if either key or value has deserialization issues.");
    throw new IllegalStateException("Method should only be invoked if either key or value has deserialization issues.");
  }

  /**
   * invokes the business error handler for the record
   *
   * @param record record with wrapped key and value objects
   * @param e exception that has been thrown during business processing
   * @return true if processing should stop
   */
  private boolean handleErrorWithErrorHandler(
      ConsumerRecord<K, V> record,
      RuntimeException e) {

    // use default error handling, if not explicit error handler is given
    // Caution, error handler is null here - already considers deprecation
    return errorHandler.handleError(record, e, null);
  }


  /**
   * Processing of dead letter handling. The message is inserted into the retry topic if maximum
   * number retries has not been reached. Otherwise it will be sent to dead letter topic.
   */
  private void deadLetterHandling(byte[] key, byte[] value, Headers headers, Exception e) {
    final int executedNumberOfRetries = getNumberRetries(headers) + 1;

    Headers headersList = new RecordHeaders();
    headersList.add(EXCEPTION, serialize(e));
    headersList.add(RETRIES, serialize(executedNumberOfRetries));

    if (executedNumberOfRetries <= maxNumberOfRetries) {
      retryProducer.send(key, value, headersList);
    } else {
      deadLetterTopicProducer.send(key, value, headersList);
    }
  }

  @Override
  public void commitOnClose(
      KafkaConsumer<DeserializerResult<K>, DeserializerResult<V>> consumer) {
    try {
      consumer.commitSync();
    } catch (CommitFailedException e) {
      LOGGER.error("Commit failed", e);
    }
  }

  @Override
  public void verifyConsumerConfig(Map<String, String> config) {
    if (Boolean.parseBoolean(config.getOrDefault("enable.auto.commit", "false"))) {
      throw new ConfigurationException(
          "The strategy should not auto commit since the strategy does that manually. But property 'enable.auto.commit' in consumer config is set to 'true'");
    }
  }

  private int getNumberRetries(Headers headers) {
    for (final Header pair : headers) {
      if (RETRIES.equals(pair.key()) && pair.value() != null) {
        Object deserializedValue = deserialize(pair.value());
        if (deserializedValue instanceof Integer) {
          return (int) deserializedValue;
        }
      }
    }
    return 0;
  }

  @SuppressWarnings("deprecation") // record.checksum() is deprecated as of Kafka 0.11.0
  private ConsumerRecord<K, V> createKvRecord(
      ConsumerRecord<DeserializerResult<K>, DeserializerResult<V>> record) {
    final K recordVKey = (record.key() != null && record.key().parsedSuccessful()) ? record.key().getParsedValue() : null;
    final V recordValue = (record.value() != null && record.value().parsedSuccessful()) ? record.value().getParsedValue() : null;
    return new ConsumerRecord<>(
        record.topic(), record.partition(), record.offset(),
        record.timestamp(), record.timestampType(), record.checksum(), // NOSONAR
        record.serializedKeySize(),
        record.serializedValueSize(),
        recordVKey, recordValue, record.headers());
  }

  @Override
  public void close() {
    retryListener.forEach(MessageListener::stopConsumer);
  }
}
