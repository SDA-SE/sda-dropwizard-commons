package org.sdase.commons.server.opa.extension;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.ws.rs.container.ContainerRequestContext;

/**
 * An extension to provide additional data to the {@link
 * org.sdase.commons.server.opa.filter.model.OpaInput} before sending it to the Open Policy Agent.
 *
 * <p>Implementing this class should be the <i>last resort</i>. It is more favourable to depend on
 * the existing {@link org.sdase.commons.server.opa.extension extensions} and instead use the
 * constraint feature of the {@link
 * org.sdase.commons.server.opa.OpaJwtPrincipal#getConstraintsAsEntity(Class)} to receive data from
 * the policy decider and use it to decide based on it in your service.
 *
 * <p>Each extension is added in an own namespace, that means that it's data is accessible in a
 * single sub-property. This contents can be represented by any object that is serializable by an
 * {@link ObjectMapper}.
 *
 * <p>Example that returns a boolean:
 *
 * <pre>
 *   {
 *     "jwt": "…",
 *     "path": ["…", "…"],
 *     "httpMethod": "GET",
 *     "myExtension": true
 *   }
 * </pre>
 *
 * <p>Example that returns an object:
 *
 * <pre>
 *   {
 *     "jwt": "…",
 *     "path": ["…", "…"],
 *     "httpMethod": "GET",
 *     "myExtension": {
 *       "myBoolean": true,
 *       "myString": "asdf",
 *       "myArray": ["…", "…", "…"]
 *     }
 *   }
 * </pre>
 *
 * The property name in the input is defined during registration in {@link
 * org.sdase.commons.server.opa.OpaBundle.Builder#withInputExtension(String, OpaInputExtension)}.
 */
public interface OpaInputExtension<T> {
  /**
   * When registered, it is called in {@link
   * org.sdase.commons.server.opa.filter.OpaAuthFilter#filter(ContainerRequestContext)}. The return
   * value is added as child of the property name defined during the {@link
   * org.sdase.commons.server.opa.OpaBundle.Builder#withInputExtension(String, OpaInputExtension)
   * registration}.
   *
   * <p>Example that adds the property {@code "myExtension": true}:
   *
   * <pre>
   *   public class OpaInputHeadersExtension implements OpaInputExtension&lt;Boolean&gt; {
   *     &#64;Override
   *     public Boolean createAdditionalInputContent(ContainerRequestContext requestContext) {
   *       return true;
   *     }
   *   }
   *
   *   // ... in your application
   *   OpaBundle.builder()
   *       // ...
   *       .withInputExtension("myExtension", new MyOpaExtension())
   *       .build();
   *   // ...
   * </pre>
   *
   * @param requestContext the request context
   * @return the JsonNode that should be added as child of the extension's namespace property.
   */
  T createAdditionalInputContent(ContainerRequestContext requestContext);
}
