package org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.sdase.commons.server.kafka.consumer.ErrorHandler;
import org.sdase.commons.server.kafka.consumer.MessageHandler;
import org.sdase.commons.server.kafka.producer.MessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used for sending messages to a kafka topic with a specified delay. The message handler thread will sleep for a given time if message  timestamp is not smaller than current time - delay
 *
 *
 */
public class RetryHandler implements ErrorHandler<byte[],byte[]>, MessageHandler<byte[],byte[]> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryHandler.class);

    private final long retryIntervalMS;
    private final MessageProducer<byte[], byte[]> sourceTopicProducer;

    /**
     *
     * @param sourceTopicProducer producer used to send records back to application
     * @param intervalMS interval for the delayed message delivery
     */
    public RetryHandler(MessageProducer<byte[], byte[]> sourceTopicProducer, long intervalMS) {
        this.sourceTopicProducer = sourceTopicProducer;
        this.retryIntervalMS = intervalMS;
    }

    /**
     * Stops the consumer for the retry interval and then inserts the record to the main topic.
     * By doing so, the retry is triggered not before retry interval expires.
     *
     * @param consumerRecord record that has caused an exception,
     * @param e error that occured during processing
     * @param consumer consumer that read the record
     * @return if the processing should stop
     */
    @Override
    public boolean handleError(ConsumerRecord<byte[], byte[]> consumerRecord, RuntimeException e, Consumer<byte[], byte[]> consumer) {
        if (e instanceof DelayRetryException) {
            consumer.pause(consumer.assignment());
            final long threadId = Thread.currentThread().getId();

            try {
                Thread.sleep(retryIntervalMS);
            } catch (InterruptedException ex) { //NOSONAR
                LOGGER.error("Thread {} was interrupted", threadId, ex);
            }

            consumer.resume(consumer.paused());
            sourceTopicProducer
                .send(consumerRecord.key(), consumerRecord.value(), consumerRecord.headers());

        } else {
            LOGGER.warn("Unexpected exception thrown and ignored during retry handling", e);
            // rethrow exception, since SyncCommitMLS does not commit messages that are not
            // completed successfully.
            throw e;
        }
        return true;
    }

    /**
     * Processes a record read from retry topic.
     * If the timestamp of the message is not smaller than current time minus retry interval, an
     * {@link DelayRetryException} is thrown to force the invocation of the error handler that again
     * invokes the 'handleError' method.
     * @param consumerRecord record to be processed
     */
    @Override
    public void handle(ConsumerRecord<byte[], byte[]> consumerRecord) {
        if (consumerRecord.timestamp() < (System.currentTimeMillis() - retryIntervalMS)) {
            sourceTopicProducer.send(consumerRecord.key(), consumerRecord.value(), consumerRecord.headers());
        } else {
            throw new DelayRetryException(retryIntervalMS);
        }
    }
}
