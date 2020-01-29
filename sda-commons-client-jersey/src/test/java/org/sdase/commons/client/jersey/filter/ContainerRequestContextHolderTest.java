package org.sdase.commons.client.jersey.filter;

import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.sdase.commons.client.jersey.filter.ContainerRequestContextHolder.transferRequestContext;

import java.net.URI;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ContainerRequest;
import org.junit.Test;
import org.slf4j.MDC;

public class ContainerRequestContextHolderTest {

  @Test
  public void shouldTransferRequestContextToThreadForRunnable()
      throws InterruptedException, ExecutionException {
    initializeContext();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(transferRequestContext(() -> {
      assertThat(MDC.get("Trace-Token")).isEqualTo("a-trace-token");
      assertThat(new AuthHeaderClientFilter().getHeaderValue().orElse(null)).isEqualTo("an-access-token");
    })).get();

    executorService.shutdown();
  }

  @Test
  public void shouldTransferRequestContextToThreadForCallable()
      throws ExecutionException, InterruptedException {
    initializeContext();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    int result = executorService.submit(transferRequestContext(() -> {
      assertThat(MDC.get("Trace-Token")).isEqualTo("a-trace-token");
      assertThat(new AuthHeaderClientFilter().getHeaderValue().orElse(null)).isEqualTo("an-access-token");
      return 42;
    })).get();

    assertThat(result).isEqualTo(42);

    executorService.shutdown();
  }

  @Test
  public void shouldCleanupAfterTransferRequestContextToThreadForRunnable()
      throws InterruptedException, ExecutionException {
    initializeContext();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(transferRequestContext(() -> {})).get();
    executorService.submit(() -> {
      // As the thread is reused, we expect it to be cleaned.
      assertThat(MDC.get("Trace-Token")).isNull();
      assertThat(new AuthHeaderClientFilter().getHeaderValue().isPresent()).isFalse();
    }).get();

    executorService.shutdown();
  }

  @Test
  public void shouldCleanupAfterTransferRequestContextToThreadForCallable()
      throws ExecutionException, InterruptedException {
    initializeContext();

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.submit(transferRequestContext(() -> {})).get();
    executorService.submit(() -> {
      // As the thread is reused, we expect it to be cleaned.
      assertThat(MDC.get("Trace-Token")).isNull();
      assertThat(new AuthHeaderClientFilter().getHeaderValue().isPresent()).isFalse();
      return 42;
    }).get();

    executorService.shutdown();
  }

  private void initializeContext() {
    MDC.put("Trace-Token", "a-trace-token");
    ContainerRequest containerRequest = new ContainerRequest(
        URI.create("http://example.com"), URI.create("http://example.com/path"), "PUT", null,
        new MapPropertiesDelegate());
    containerRequest.header(AUTHORIZATION, "an-access-token");
    new ContainerRequestContextHolder().filter(containerRequest);
  }
}
