package org.sdase.commons.server.jackson.hal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sdase.commons.server.jackson.hal.HalLinkProvider.linkTo;
import static org.sdase.commons.server.jackson.hal.HalLinkProvider.methodOn;

import io.openapitools.jackson.dataformat.hal.HALLink;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import java.net.URI;
import org.junit.jupiter.api.Test;

class HalLinkProviderTest {

  @Test
  void shouldProvideHalLinkForNormalPathParams() {
    final LinkResult linkResult = linkTo(methodOn(TestApi.class).testMethod("TEST"));
    assertLinkResult(linkResult, "/testPath/TEST");
  }

  @Test
  void shouldFailWhenNoInterfaceIsProvided() {
    assertThatThrownBy(HalLinkProviderTest::methodWithNoInterfaceProvided)
        .isInstanceOf(HalLinkMethodInvocationException.class);
  }

  @Test
  void shouldProvideHalLinkForQueryParam() {
    final LinkResult linkResult = linkTo(methodOn(TestApi.class).testMethodQueryParam("TEST"));
    assertLinkResult(linkResult, "/testPath?testRequestParam=TEST");
  }

  @Test
  void shouldProvideHalLinkIgnoringCookieParam() {
    final LinkResult linkResult = linkTo(methodOn(TestApi.class).testMethodCookieParam("TEST"));
    assertLinkResult(linkResult, "/testPath");
  }

  @Test
  void shouldProvideHalLinkForDetailed() {
    final LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodDetail("TEST", 1, "testTheQuery"));
    assertLinkResult(linkResult, "/testPath/TEST/detail/testTheQuery?query=1");
  }

  @Test
  void shouldProvideHalLinkForDetailedAndIgnoreQueryParamWithNullValue() {
    final LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodDetail("TEST", null, "testTheQuery"));
    assertLinkResult(linkResult, "/testPath/TEST/detail/testTheQuery");
  }

  @Test
  void shouldFailWithoutAnnotation() {
    assertThatThrownBy(HalLinkProviderTest::methodWithoutPathParamAnnotation)
        .isInstanceOf(HalLinkMethodInvocationException.class);
  }

  @Test
  void shouldDoNothingWhenNoParamsAreProvided() {
    final LinkResult linkResult = linkTo(methodOn(TestApi.class).testMethodWithoutParams());
    assertLinkResult(linkResult, "/testPathWithNoParams");
  }

  @Test
  void shouldFailWithNonProxiedMethod() {
    assertThatThrownBy(() -> linkTo("testMethod"))
        .isInstanceOf(HalLinkMethodInvocationException.class);
  }

  @Test
  void shouldFailWithNullProxiedMethod() {
    assertThatThrownBy(() -> linkTo(null))
        .isInstanceOf(HalLinkMethodInvocationException.class)
        .hasMessageContaining("No proxied method invocation processed.");
  }

  private void assertLinkResult(LinkResult linkResult, String expectedPath) {
    final HALLink halLink = linkResult.asHalLink();
    final URI uri = linkResult.asUri();
    assertThat(halLink).isNotNull().extracting("href").isEqualTo(expectedPath);
    assertThat(uri).isNotNull().hasToString(expectedPath);
  }

  private static LinkResult methodWithNoInterfaceProvided() {
    return linkTo(methodOn(TestController.class).testMethod("FAIL"));
  }

  private static LinkResult methodWithoutPathParamAnnotation() {
    return linkTo(methodOn(TestApi.class).testMethodWithoutPathParamAnnotation("FAIL"));
  }
}

@Path("")
interface TestApi {
  @Path("/testPath/{testArg}")
  @GET
  String testMethod(@PathParam("testArg") String testArg);

  @Path("/testPath/{testArg}/detail/{testArg2}")
  @GET
  String testMethodDetail(
      @PathParam("testArg") String testArg,
      @QueryParam("query") Integer testArgTwo,
      @PathParam("testArg2") String query);

  @Path("/testPath")
  @GET
  String testMethodCookieParam(@CookieParam("testCookie") String testCookie);

  @Path("/testPath")
  @GET
  String testMethodQueryParam(@QueryParam("testRequestParam") String testArg);

  @Path("/testPathWithNoParams")
  @GET
  String testMethodWithoutParams();

  @Path("/testPath/{testArg}")
  @GET
  String testMethodWithoutPathParamAnnotation(String testArg);
}

@Path("")
class TestController {

  @Path("/testPath/{testArg}")
  @GET
  public String testMethod(@PathParam("testArg") String testArg) {
    return null;
  }
}
