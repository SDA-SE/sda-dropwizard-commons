package org.sdase.commons.server.kafka.consumer.strategies.deadletter.retry;

/**
 * Exception thrown if no messages for retry are left in the retry topic or wait period has not
 * expired yet.
 * <p>This will force the strategy to invoke the error handler</p>
 *
 */
class DelayRetryException extends RuntimeException {

     DelayRetryException(long intervalMS){
        super("Message delivery will be delayed for: " + intervalMS);
    }

}
