package org.sdase.commons.client.jersey.proxy.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sdase.commons.client.jersey.error.ClientRequestException;

/**
 * Annotation used to suppress {@link ClientRequestException} with a {@link
 * javax.ws.rs.WebApplicationException} cause in API clients. Instead of throwing an exception,
 * {@code null} will be returned.
 *
 * <p>This annotation is only suitable for methods that do not return a {@link
 * javax.ws.rs.core.Response}. This annotation is NOT suitable for declared default implementations.
 *
 * <p>Suppressing expected errors (e.g. 404) and not catching any exception can simplify consumer
 * code, as the exception of a not listed code is automatically converted to a 500 Internal Server
 * Error by the {@link org.sdase.commons.client.jersey.error.ClientRequestExceptionMapper} and the
 * consumer code can easily check for a {@code null} value instead of catching and checking the
 * exception. However, consumers may still catch the {@link ClientRequestException} to implement
 * additional behaviour for not suppressed errors, e.g. passing a 401 to their client.
 *
 * <p>In case a {@linkplain #value() defined error} occurs, the annotated method will return {@code
 * null} instead of throwing a {@link ClientRequestException}.
 *
 * <p>{@link ClientRequestException}s will be thrown for HTTP errors that are not specified as if
 * the method is not annotated.
 *
 * <p>By default no error is suppressed. Users may suppress {@linkplain #allRedirectErrors() all
 * redirect errors (3xx)}, {@linkplain #allClientErrors() all client errors (4xx)}, {@linkplain
 * #allServerErrors() all server errors (5xx)} or {@linkplain #value() specific error codes}.
 *
 * @see SuppressConnectTimeoutErrorsToNull
 * @see SuppressReadTimeoutErrorsToNull
 * @see SuppressProcessingErrorsToNull
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuppressHttpErrorsToNull {

  /**
   * @return the <a href="https://en.wikipedia.org/wiki/List_of_HTTP_status_codes">HTTP status
   *     codes</a> that should be suppressed. Exceptions of other status codes will not be
   *     suppressed.
   */
  int[] value() default {};

  /**
   * @return {@code true}, if {@linkplain #value() the individually defined codes} are extended by
   *     all redirect errors (3xx).
   */
  boolean allRedirectErrors() default false;

  /**
   * @return {@code true}, if {@linkplain #value() the individually defined codes} are extended by
   *     all {@linkplain ClientRequestException#isClientError() client errors (4xx)}.
   */
  boolean allClientErrors() default false;

  /**
   * @return {@code true}, if {@linkplain #value() the individually defined codes} are extended by
   *     all {@linkplain ClientRequestException#isServerError() server errors (5xx)}.
   */
  boolean allServerErrors() default false;
}
