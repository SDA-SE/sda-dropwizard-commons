package org.sdase.commons.client.jersey.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import org.codefetti.proxy.handler.InterfaceProxyBuilder;
import org.sdase.commons.client.jersey.error.ClientRequestException;
import org.sdase.commons.client.jersey.proxy.annotation.SuppressConnectTimeoutErrorsToNull;
import org.sdase.commons.client.jersey.proxy.annotation.SuppressHttpErrorsToNull;
import org.sdase.commons.client.jersey.proxy.annotation.SuppressProcessingErrorsToNull;
import org.sdase.commons.client.jersey.proxy.annotation.SuppressReadTimeoutErrorsToNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApiClientInvocationHandler implements InvocationHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ApiClientInvocationHandler.class);

  private static final List<Integer> ALL_REDIRECT_ERRORS =
      IntStream.rangeClosed(300, 399).boxed().collect(Collectors.toList());
  private static final List<Integer> ALL_CLIENT_ERRORS =
      IntStream.rangeClosed(400, 499).boxed().collect(Collectors.toList());
  private static final List<Integer> ALL_SERVER_ERRORS =
      IntStream.rangeClosed(500, 599).boxed().collect(Collectors.toList());

  private final Object delegate;

  /**
   * Creates a proxy around the given {@code jerseyClientProxy} that wraps all {@link
   * WebApplicationException}s in {@link ClientRequestException}s.
   *
   * @param apiInterface the client interface
   * @param jerseyClientProxy the proxy instance build by {@code
   *     WebResourceFactory.newResource(Class<C> resourceInterface, ...)}
   * @param <T> the client interface
   * @return a proxy around the {@code jerseyClientProxy}
   */
  public static <T> T createProxy(Class<T> apiInterface, T jerseyClientProxy) {
    ApiClientInvocationHandler clientInvocationHandler =
        new ApiClientInvocationHandler(jerseyClientProxy);
    return InterfaceProxyBuilder.createProxy(apiInterface, clientInvocationHandler);
  }

  private ApiClientInvocationHandler(Object delegate) {
    this.delegate = delegate;
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    try {
      return method.invoke(delegate, args);
    } catch (InvocationTargetException invocationTargetException) {
      Throwable cause = invocationTargetException.getCause();
      if (cause instanceof WebApplicationException) {
        throwIfNotSuppressed(method, new ClientRequestException(cause));
      } else if (cause instanceof ProcessingException) {
        throwIfNotSuppressed(method, new ClientRequestException(cause));
      } else {
        throw cause;
      }
      LOG.info(
          "API client error '{}' suppressed to null when calling {}", cause.getMessage(), method);
      return null;
    }
  }

  /**
   * @throws ClientRequestException the given {@code exceptionToThrow} if not suppressed by
   *     annotations of the {@code invokedMethod}.
   */
  private void throwIfNotSuppressed(Method invokedMethod, ClientRequestException exceptionToThrow) {
    Optional<Integer> httpStatus = getHttpStatus(exceptionToThrow);
    if (httpStatus.isPresent()) {
      if (!isHttpStatusSuppressed(invokedMethod, httpStatus.get())) {
        throw exceptionToThrow;
      }
    } else if (exceptionToThrow.isConnectTimeout()) {
      if (!isConnectTimeoutErrorSuppressed(invokedMethod)) {
        throw exceptionToThrow;
      }
    } else if (exceptionToThrow.isReadTimeout()) {
      if (!isReadTimeoutErrorSuppressed(invokedMethod)) {
        throw exceptionToThrow;
      }
    } else if (exceptionToThrow.isProcessingError()) {
      if (!isProcessingErrorSuppressed(invokedMethod)) {
        throw exceptionToThrow;
      }
    } else {
      throw exceptionToThrow;
    }
    exceptionToThrow.close();
  }

  private Optional<Integer> getHttpStatus(ClientRequestException exceptionToThrow) {
    return exceptionToThrow.getResponse().map(Response::getStatus);
  }

  private boolean isHttpStatusSuppressed(Method invokedMethod, int httStatusCode) {
    SuppressHttpErrorsToNull suppressedHttpErrors =
        invokedMethod.getDeclaredAnnotation(SuppressHttpErrorsToNull.class);
    if (suppressedHttpErrors == null) {
      return false;
    }
    Set<Integer> suppressedStatusCodes =
        IntStream.of(suppressedHttpErrors.value()).boxed().collect(Collectors.toSet());
    if (suppressedHttpErrors.allRedirectErrors()) {
      suppressedStatusCodes.addAll(ALL_REDIRECT_ERRORS);
    }
    if (suppressedHttpErrors.allClientErrors()) {
      suppressedStatusCodes.addAll(ALL_CLIENT_ERRORS);
    }
    if (suppressedHttpErrors.allServerErrors()) {
      suppressedStatusCodes.addAll(ALL_SERVER_ERRORS);
    }
    return suppressedStatusCodes.contains(httStatusCode);
  }

  private boolean isProcessingErrorSuppressed(Method invokedMethod) {
    return invokedMethod.isAnnotationPresent(SuppressProcessingErrorsToNull.class);
  }

  private boolean isConnectTimeoutErrorSuppressed(Method invokedMethod) {
    return invokedMethod.isAnnotationPresent(SuppressConnectTimeoutErrorsToNull.class);
  }

  private boolean isReadTimeoutErrorSuppressed(Method invokedMethod) {
    return invokedMethod.isAnnotationPresent(SuppressReadTimeoutErrorsToNull.class);
  }
}
