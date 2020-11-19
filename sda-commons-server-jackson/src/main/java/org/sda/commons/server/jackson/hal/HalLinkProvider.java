package org.sda.commons.server.jackson.hal;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Helper utility which is registered as singleton instance in the jersey environment via the
 * {@linkplain org.sdase.commons.server.jackson.JacksonConfigurationBundle}. It provides the
 * context-based HALLink for a JAX-RS interface using the {@linkplain javax.ws.rs.PathParam} and
 * {@linkplain javax.ws.rs.QueryParam} annotations.
 *
 * <p>Usage:
 *
 * <pre>
 * &#064;Path("")
 * interface TestApi {
 *  &#064;Path("/testPath/{testArg}")
 *  &#064;GET
 *  String testMethod(@PathParam("testArg") String testArg, @QueryParam("query") query);
 * }
 *
 * // Get the generated HALLink
 * HALLink HalLink = linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed")).asHalLink();
 * // Get the generated URI
 * URI Uri = linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed")).asURI();
 * </pre>
 *
 * <p>The example would create the following URI: {@code
 * "baseUri/testPath/ResourceID?query=detailed"}
 *
 * <p>The builder will use the `UriInfo` of the current request context to create absolute URIs. If
 * no request context is available, it will fall back to a simple path without host that does not
 * include any configured root or context path.
 *
 * @deprecated this package has been created by mistake. The {@code HalLinkProvider} moved to {@link
 *     org.sdase.commons.server.jackson.hal.HalLinkProvider}, please update the imports.
 */
@Deprecated
public class HalLinkProvider implements Feature {

  /**
   * @deprecated use {@link org.sdase.commons.server.jackson.hal.HalLinkProvider#linkTo(Object)}
   *     instead
   */
  @Deprecated
  public static LinkResult linkTo(Object invocation) {
    return new LinkResult(
        org.sdase.commons.server.jackson.hal.HalLinkProvider.linkTo(invocation).asUri());
  }

  /**
   * @deprecated use {@link org.sdase.commons.server.jackson.hal.HalLinkProvider#linkTo(Object)}
   *     instead
   */
  @Deprecated
  public static <T> T methodOn(Class<T> type) {
    return org.sdase.commons.server.jackson.hal.HalLinkProvider.methodOn(type);
  }

  @Override
  public boolean configure(FeatureContext context) {
    return true;
  }
}
