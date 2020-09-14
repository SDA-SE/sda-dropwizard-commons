package org.sda.commons.server.jackson.hal;

import io.openapitools.jackson.dataformat.hal.HALLink;
import io.openapitools.jackson.dataformat.hal.HALLink.Builder;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;
import org.sda.commons.server.jackson.hal.HalLinkInvocationStateUtility.MethodInvocationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper utility that can be registered in the Jersey environment to be injected into services. It
 * provides the context-based HALLink for a JAX-RS interface using the {@linkplain
 * javax.ws.rs.PathParam} and {@linkplain javax.ws.rs.QueryParam} annotations.
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
 * HALLink link = halLinkProvider.linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed"));
 * </pre>
 *
 * Would create a HALLink with the following URL: {@code
 * "contextBasePath/testPath/ResourceID?query=detailed"}
 */
public class HalLinkProvider implements Feature {

  private static final Logger LOG = LoggerFactory.getLogger(HalLinkProvider.class);

  @Context private UriInfo uriInfo;

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
   * HALLink link = halLinkProvider.linkTo(methodOn(TestApi.class).testMethod("ResourceID", "detailed"));
   * </pre>
   *
   * The example would create a HALLink with the following URL: {@code
   * "contextBasePath/testPath/ResourceID?query=detailed"}
   *
   * @param invocation the invocation placeholder.
   * @return the generated HALLink based on the method invocation
   * @throws HalLinkMethodInvocationException - If no method invocation is provided via {@linkplain
   *     HalLinkProvider#methodOn(Class)}
   */
  public HALLink linkTo(Object invocation) {
    final MethodInvocationState methodInvocationState =
        HalLinkInvocationStateUtility.loadMethodInvocationState();
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
    final HALLink link = new Builder(uriBuilder.build()).build();
    HalLinkInvocationStateUtility.unloadMethodInvocationState();
    return link;
  }

  /**
   * Creates and returns a proxy instance based on the passed type parameter with a method
   * invocation handler, which processes and saves the needed method invocation information in the
   * current thread. Parameters in the afterwards called method of the proxy will be used to resolve
   * the URI template of the corresponding method.
   *
   * <p>After the method invocation of the proxy instance the outer method {@linkplain
   * HalLinkProvider#linkTo(Object)} will process the derived information of the invocation to *
   * create the HALLink.
   *
   * <p>It should be ensured that the parameters are annotated with {@linkplain PathParam} or with
   * {@linkplain QueryParam}. The passed class type must represent interfaces, not classes or
   * primitive types.
   *
   * @param <T> the type parameter based on the passed type. Should not be null {@literal null}.
   * @param type the type on which the method should be invoked.
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
}
