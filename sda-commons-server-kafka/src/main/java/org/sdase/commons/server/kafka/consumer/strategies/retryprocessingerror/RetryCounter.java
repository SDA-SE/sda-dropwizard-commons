package org.sdase.commons.server.kafka.consumer.strategies.retryprocessingerror;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * Helper class counting the number of executions for a given record.
 *
 * <p>Internally it increases a counter for the current offset of the record as long as the offset
 * remains the same. If the offset changes, the counter is reset to 0.
 */
public class RetryCounter {
  private final Map<Integer, OffsetCounter> offsetCountersByPartition =
      Collections.synchronizedMap(new HashMap<>());

  private final long maxRetryCount;

  public RetryCounter(long maxRetryCount) {
    this.maxRetryCount = maxRetryCount;
  }

  public void incErrorCount(ConsumerRecord<?, ?> consumerRecord) {
    getOffsetCounterInternal(consumerRecord).inc();
  }

  public boolean isMaxRetryCountReached(ConsumerRecord<?, ?> consumerRecord) {
    long counter = getOffsetCounter(consumerRecord);
    return counter > maxRetryCount;
  }

  public long getOffsetCounter(ConsumerRecord<?, ?> consumerRecord) {
    return getOffsetCounterInternal(consumerRecord).count;
  }

  public long getMaxRetryCount() {
    return maxRetryCount;
  }

  private OffsetCounter getOffsetCounterInternal(ConsumerRecord<?, ?> consumerRecord) {
    OffsetCounter counter =
        offsetCountersByPartition.computeIfAbsent(
            consumerRecord.partition(), k -> new OffsetCounter(0));
    if (counter.offset != consumerRecord.offset()) {
      counter.offset = consumerRecord.offset();
      counter.count = 0;
    }
    return counter;
  }

  private static class OffsetCounter {
    private long offset = 0;
    private long count = 0;

    public OffsetCounter(long offset) {
      this.offset = offset;
      this.count = 0;
    }

    public long inc() {
      return ++count;
    }
  }
}
