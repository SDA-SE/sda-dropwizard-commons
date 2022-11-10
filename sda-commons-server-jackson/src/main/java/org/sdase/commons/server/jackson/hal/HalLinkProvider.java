package org.sdase.commons.server.jackson.hal;

import io.openapitools.jackson.dataformat.hal.HALLink;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility which is registered as singleton instance in the jersey environment via the
 * {@linkplain org.sdase.commons.server.jackson.JacksonConfigurationBundle}. It provides the
 * context-based HALLink for a JAX-RS interface using the {@linkplain PathParam} and {@linkplain
 * QueryParam} annotations.
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
 */
public class HalLinkProvider implements Feature {

  private static final Logger LOG = LoggerFactory.getLogger(HalLinkProvider.class);
  private static HalLinkProvider instance;

  @Context private UriInfo uriInfo;

  private HalLinkProvider() {}

  /**
   * Creates a {@linkplain HALLink} based on JAX-RS annotated parameters of an interface. This
   * method requires a second entrypoint with {@linkplain
   * HalLinkInvocationStateUtility#methodOn(Class)} to process the annotated JAX-RS interface with
   * the corresponding {@linkplain PathParam} and {@linkplain QueryParam} and the passed arguments
   * to the proxied method invocation.
   *
   * <p>Example:
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
   * HALLink halLink = linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed")).asHalLink();
   * // Get the generated URI
   * URI uri = linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed")).asURI();
   * </pre>
   *
   * <p>The example would create the following URI: {@code
   * "baseUri/testPath/ResourceID?query=detailed"}
   *
   * <p>The builder will use the `UriInfo` of the current request context to create absolute URIs.
   * If no request context is available, it will fall back to a simple path without host that does
   * not include any configured root or context path.
   *
   * @param invocation the invocation placeholder.
   * @return the generated {@linkplain LinkResult} based on the method invocation
   * @throws HalLinkMethodInvocationException - If no method invocation is provided via {@linkplain
   *     HalLinkProvider#methodOn(Class)}
   */
  public static LinkResult linkTo(Object invocation) {
    return getInstance().linkToInvocation(invocation);
  }

  private LinkResult linkToInvocation(Object invocation) {
    final HalLinkInvocationStateUtility.MethodInvocationState methodInvocationState =
        HalLinkInvocationStateUtility.loadMethodInvocationState();
    try {
      if (invocation != null || !methodInvocationState.isProcessed()) {
        throw new HalLinkMethodInvocationException("No proxied method invocation processed.");
      }
      final UriBuilder uriBuilder =
          baseUriBuilder()
              .path(methodInvocationState.getType())
              .path(methodInvocationState.getType(), methodInvocationState.getInvokedMethod())
              // Add Path Params from invocation state
              .resolveTemplates(methodInvocationState.getPathParams());
      // Add Query Params from invocation state
      methodInvocationState.getQueryParams().forEach(uriBuilder::queryParam);
      LinkResult linkResult = new LinkResult(uriBuilder.build());
      HalLinkInvocationStateUtility.unloadMethodInvocationState();
      return linkResult;
    } catch (IllegalArgumentException e) {
      throw new HalLinkMethodInvocationException("Could not build URI.", e);
    }
  }

  /**
   * Creates and returns a proxy instance based on the passed type parameter with a method
   * invocation handler, which processes and saves the needed method invocation information in the
   * current thread. Parameters in the afterwards called method of the proxy will be used to resolve
   * the URI template of the corresponding method. The called method of the interface must have a
   * return type, which means it should not be of type {@code void}.
   *
   * <p>After the method invocation of the proxy instance the outer method {@linkplain
   * HalLinkProvider#linkTo(Object)} will process the derived information of the invocation to
   * create the HALLink.
   *
   * <p>It should be ensured that the parameters are annotated with {@linkplain PathParam} or with
   * {@linkplain QueryParam}. The passed class type must represent interfaces, not classes or
   * primitive types.
   *
   * @param <T> the type parameter based on the passed type.
   * @param type the type on which the method should be invoked must be an interface.
   * @return the proxy instance
   * @throws HalLinkMethodInvocationException if the proxy instance could not be created
   */
  public static <T> T methodOn(Class<T> type) {
    return HalLinkInvocationStateUtility.methodOn(type);
  }

  private UriBuilder baseUriBuilder() {
    try {
      return uriInfo.getBaseUriBuilder();
    } catch (Exception e) {
      LOG.error(
          "Unable to access baseUriBuilder from request context. Using context unaware builder as a fallback.",
          e);
      return new JerseyUriBuilder().path("/");
    }
  }

  @Override
  public boolean configure(FeatureContext context) {
    return true;
  }

  /**
   * Returns the singleton instance of the HalLinkProvider
   *
   * @return the HalLinProvider instance
   */
  public static synchronized HalLinkProvider getInstance() {
    if (instance == null) {
      instance = new HalLinkProvider();
    }
    return instance;
  }
}
