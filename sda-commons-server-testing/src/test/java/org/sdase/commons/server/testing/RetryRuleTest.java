package org.sdase.commons.server.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Rule;
import org.junit.Test;

public class RetryRuleTest {
  private final AtomicInteger shouldRetryOnAssertion = new AtomicInteger(0);
  private final AtomicInteger shouldNotRetryOnSuccess = new AtomicInteger(0);

  @Rule public final RetryRule retryRule = new RetryRule();

  @Test()
  @Retry(6)
  public void shouldRetryOnAssertion() {
    assertThat(shouldRetryOnAssertion.incrementAndGet()).isGreaterThan(5);
  }

  @Test()
  @Retry(5)
  public void shouldNotRetryOnSuccess() {
    assertThat(shouldNotRetryOnSuccess.incrementAndGet()).isEqualTo(1);
  }
}
