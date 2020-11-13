package org.sdase.commons.client.jersey.proxy.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.sdase.commons.client.jersey.error.ClientRequestException;

/**
 * Annotation used to suppress {@link ClientRequestException} caused by {@linkplain
 * ClientRequestException#isProcessingError() processing errors}. Instead of throwing an exception,
 * {@code null} will be returned.
 *
 * <p>This annotation is only suitable for methods that do not return a {@link
 * javax.ws.rs.core.Response}. This annotation is NOT suitable for declared default implementations.
 *
 * <p>Suppressing expected errors and not catching any exception can simplify consumer code, as
 * other errors are automatically converted to a 500 Internal Server Error by the {@link
 * org.sdase.commons.client.jersey.error.ClientRequestExceptionMapper} and the consumer code can
 * easily check for a {@code null} value instead of catching and checking the exception. However,
 * consumers may still catch the {@link ClientRequestException} to implement additional behaviour
 * for not suppressed errors, e.g. passing a 401 to their client.
 *
 * @see SuppressConnectTimeoutErrorsToNull
 * @see SuppressReadTimeoutErrorsToNull
 * @see SuppressHttpErrorsToNull
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SuppressProcessingErrorsToNull {}
