package org.sdase.commons.server.dropwizard.metadata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetadataContextStaticMethodsIntegrationTest {

  @BeforeEach
  void cleanup() {
    MetadataContextHolder.clear();
  }

  @Test
  void currentContextShouldNeverBeNull() {
    assertThat(MetadataContext.current()).isNotNull();
  }

  @Test
  void detachedCurrentContextShouldNeverBeNull() {
    assertThat(MetadataContext.detachedCurrent()).isNotNull();
  }

  @Test
  void shouldProvideDetachedContext() {
    var given = detachedContextWithTenantId();
    MetadataContext.createContext(given);

    assertThat(MetadataContext.detachedCurrent())
        .isNotNull()
        .extracting(d -> d.get("tenant-id"))
        .isEqualTo(List.of("tenant-1"));
  }

  @Test
  void shouldProvideDetachedContextNotAffectingCurrentContext() {
    var given = detachedContextWithTenantId();
    MetadataContext.createContext(given);

    var actualDetached = MetadataContext.detachedCurrent();
    actualDetached.put("process-id", List.of("process-1"));
    var currentKeys = MetadataContext.current().keys();
    assertThat(currentKeys).containsOnly("tenant-id");
  }

  @Test
  void shouldCreateContextFromDetachedContext() {
    var given = detachedContextWithTenantId();
    MetadataContext.createContext(given);

    var actual = MetadataContext.current().valuesByKey("tenant-id");

    assertThat(actual).isEqualTo(List.of("tenant-1"));
  }

  @Test
  void shouldBeIndependentOfDetachedSource() {
    var given = detachedContextWithTenantId();
    MetadataContext.createContext(given);
    given.put("process-id", List.of("process-1"));

    var actual = MetadataContext.current().keys();

    assertThat(actual).containsOnly("tenant-id");
  }

  @Test
  void shouldOverwriteExistingContext() {
    var oldContext = new DetachedMetadataContext();
    oldContext.put("process-id", List.of("process-1"));
    MetadataContext.createContext(oldContext);
    var given = detachedContextWithTenantId();
    MetadataContext.createContext(given);

    var actual = MetadataContext.current().keys();

    assertThat(actual).containsOnly("tenant-id");
  }

  @Test
  void shouldTransferContextToRunnable() throws InterruptedException {
    var given = detachedContextWithTenantId();

    MetadataContext.createContext(given);
    var originalCurrent = MetadataContext.current();

    var actualTransferred = new AtomicReference<MetadataContext>();

    Runnable r = () -> actualTransferred.set(MetadataContext.current());

    var t = new Thread(MetadataContext.transferMetadataContext(r));
    t.start();
    t.join(1_000);

    assertThat(actualTransferred).hasValue(originalCurrent);
  }

  @Test
  // counter measure for shouldTransferContextToRunnable
  void shouldNotHaveContextInNewThreadWithoutTransferRunnable() throws InterruptedException {
    var given = detachedContextWithTenantId();

    MetadataContext.createContext(given);
    var originalCurrent = MetadataContext.current();

    var actualTransferred = new AtomicReference<MetadataContext>();

    var t = new Thread(() -> actualTransferred.set(MetadataContext.current()));
    t.start();
    t.join(1_000);

    assertThat(actualTransferred).hasValueMatching(v -> v != null && v != originalCurrent);
  }

  @Test
  void shouldTransferContextToCallable()
      throws InterruptedException, ExecutionException, TimeoutException {
    var given = detachedContextWithTenantId();

    MetadataContext.createContext(given);
    var originalCurrent = MetadataContext.current();

    var actualTransferred = new AtomicReference<MetadataContext>();

    Callable<Object> c = () -> actualTransferred.getAndSet(MetadataContext.current());

    var executor = Executors.newSingleThreadExecutor();
    try {
      executor.submit(MetadataContext.transferMetadataContext(c)).get(1, TimeUnit.SECONDS);
      assertThat(actualTransferred).hasValue(originalCurrent);
    } finally {
      executor.shutdown();
    }
  }

  @Test
  // counter measure for shouldTransferContextToCallable
  void shouldNotHaveContextInNewThreadWithoutTransferCallable()
      throws InterruptedException, ExecutionException, TimeoutException {
    var given = detachedContextWithTenantId();

    MetadataContext.createContext(given);
    var originalCurrent = MetadataContext.current();

    var actualTransferred = new AtomicReference<MetadataContext>();

    Callable<Object> c = () -> actualTransferred.getAndSet(MetadataContext.current());

    var executor = Executors.newSingleThreadExecutor();
    try {
      executor.submit(c).get(1, TimeUnit.SECONDS);
      assertThat(actualTransferred).hasValueMatching(v -> v != null && v != originalCurrent);
    } finally {
      executor.shutdown();
    }
  }

  private static DetachedMetadataContext detachedContextWithTenantId() {
    var given = new DetachedMetadataContext();
    given.put("tenant-id", List.of("tenant-1"));
    return given;
  }
}
