package org.sdase.commons.server.dropwizard;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Implement this interface in classes that should receive {@link javax.ws.rs.core.Context}
 * injections and are registered as instance. This interface is a convenience that restores the
 * behavior of Dropwizard 1.3.x.
 *
 * <pre>
 *   public class ExampleApp extends Application&lt;Configuration&gt; {
 *     &#64;Override
 *     public void run(Configuration configuration, Environment environment) {
 *       // if you register the endpoint as instance...
 *       environment.jersey().register(new ExampleEndpoint());
 *     }
 *
 *     &#64;Path("")
 *     public static class ExampleEndpoint implements ContextAwareEndpoint {
 *       // ...the context injection will only work if
 *       // ContextAwareEndpoint is implemented
 *       &#64;Context UriInfo uriInfo;
 *
 *       // ...
 *     }
 *   }
 * </pre>
 */
public interface ContextAwareEndpoint extends Feature {
  @Override
  default boolean configure(FeatureContext context) {
    return true;
  }
}
