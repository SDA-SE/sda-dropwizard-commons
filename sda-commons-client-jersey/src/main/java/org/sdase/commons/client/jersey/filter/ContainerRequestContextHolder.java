package org.sdase.commons.client.jersey.filter;

import io.opentracing.contrib.concurrent.TracedCallable;
import io.opentracing.contrib.concurrent.TracedRunnable;
import io.opentracing.util.GlobalTracer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import org.slf4j.MDC;

/**
 * A filter that provides the current request from a {@link ThreadLocal}. Currently it is only
 * implemented for the client filters in this package.
 */
@Priority(Priorities.USER)
public class ContainerRequestContextHolder
    implements ContainerRequestFilter, ContainerResponseFilter {

  private static final ThreadLocal<ContainerRequestContext> REQUEST_CONTEXT_HOLDER =
      new ThreadLocal<>();

  /**
   * @return The current {@link ContainerRequestContext} if the current {@link Thread} is in a
   *     request context. An empty {@code Optional} if the current thread is not in a request
   *     context.
   */
  static Optional<ContainerRequestContext> currentRequestContext() {
    return Optional.ofNullable(REQUEST_CONTEXT_HOLDER.get());
  }

  @Override
  public void filter(ContainerRequestContext requestContext) {
    REQUEST_CONTEXT_HOLDER.set(requestContext);
  }

  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
    REQUEST_CONTEXT_HOLDER.remove();
  }

  /**
   * Transfers the request context and MDC with the runnable to a new thread.
   *
   * @param runnable The runnable to wrap with the current request context and MDC.
   * @return The original runnable wrapped with code to transfer the request context and MDC.
   */
  public static Runnable transferRequestContext(Runnable runnable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    ContainerRequestContext requestContext = currentRequestContext().orElse(null);

    return new TracedRunnable(
        () -> {
          ContainerRequestContextHolder containerRequestContextHolder =
              new ContainerRequestContextHolder();
          try {
            if (contextMap != null) {
              MDC.setContextMap(contextMap);
            }
            containerRequestContextHolder.filter(requestContext);

            runnable.run();
          } finally {
            MDC.clear();
            containerRequestContextHolder.filter(requestContext, null);
          }
        },
        GlobalTracer.get());
  }

  /**
   * Transfers the request context and MDC with the callable to a new thread.
   *
   * @param callable The callable to wrap with the current request context and MDC.
   * @param <V> The return type of the callable
   * @return The original callable wrapped with code to transfer the request context and MDC.
   */
  public static <V> Callable<V> transferRequestContext(Callable<V> callable) {
    Map<String, String> contextMap = MDC.getCopyOfContextMap();
    ContainerRequestContext requestContext = currentRequestContext().orElse(null);

    return new TracedCallable<>(
        () -> {
          ContainerRequestContextHolder containerRequestContextHolder =
              new ContainerRequestContextHolder();
          try {
            if (contextMap != null) {
              MDC.setContextMap(contextMap);
            }
            containerRequestContextHolder.filter(requestContext);

            return callable.call();
          } finally {
            MDC.clear();
            containerRequestContextHolder.filter(requestContext, null);
          }
        },
        GlobalTracer.get());
  }
}
