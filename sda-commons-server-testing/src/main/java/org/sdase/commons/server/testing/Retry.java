package org.sdase.commons.server.testing;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Marks a test to be repeated if it fails the first time. Allows to specify how
 * often the test should be repeated. Requires to be used together with the
 * {@code RetryRule}.
 *
 * In case you have a flaky test, you can retry a test multiple times:
 * </p>
 * 
 * <pre>
 * <code>   &#64;Rule
 *    public final RetryRule retryRule = new RetryRule();
 *
 *    &#64;Test
 *    &#64;Retry(5)
 *    public void aFlakyTest() {
 *       ...
 *    }
 * </code>
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ METHOD, ANNOTATION_TYPE })
public @interface Retry {
   int value() default 1;
}
