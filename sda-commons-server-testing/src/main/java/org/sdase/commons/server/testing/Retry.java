package org.sdase.commons.server.testing;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test to be repeated if it fails the first time. Allows to specify how often the test
 * should be repeated. Requires to be used together with the {@link RetryRule}.
 *
 * <p>In case you have a flaky test, you can retry a test multiple times:
 *
 * <pre>
 *    &#64;Rule
 *    public final RetryRule retryRule = new RetryRule();
 *
 *    &#64;Test
 *    &#64;Retry(5)
 *    public void aFlakyTest() {
 *       ...
 *    }
 * </pre>
 *
 * <p>Please note that the retry rule must be used as {@code @Rule} and not as {@code @ClassRule}
 *
 * @deprecated Please migrate to Junit 5 and use {@link org.junitpioneer.jupiter.RetryingTest}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, ANNOTATION_TYPE})
@Deprecated
public @interface Retry {
  int value() default 1;
}
