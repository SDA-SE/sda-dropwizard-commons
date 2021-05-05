package org.sdase.commons.server.jackson.hal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.sda.commons.server.jackson.hal.HalLinkProvider.linkTo;
import static org.sda.commons.server.jackson.hal.HalLinkProvider.methodOn;

import io.openapitools.jackson.dataformat.hal.HALLink;
import java.net.URI;
import javax.ws.rs.CookieParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import org.junit.Test;
import org.sda.commons.server.jackson.hal.HalLinkMethodInvocationException;
import org.sda.commons.server.jackson.hal.LinkResult;

public class HalLinkProviderTest {

  @Test
  public void shouldProvideHalLinkForNormalPathParams() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethod("TEST"));
    assertLinkResult(linkResult, "/testPath/TEST");
  }

  @Test
  public void shouldFailWhenNoInterfaceIsProvided() {
    assertThatThrownBy(() -> linkTo(methodOn(TestController.class).testMethod("FAIL")))
        .isInstanceOf(org.sda.commons.server.jackson.hal.HalLinkMethodInvocationException.class);
  }

  @Test
  public void shouldProvideHalLinkForQueryParam() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodQueryParam("TEST"));
    assertLinkResult(linkResult, "/testPath?testRequestParam=TEST");
  }

  @Test
  public void shouldProvideHalLinkIgnoringCookieParam() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodCookieParam("TEST"));
    assertLinkResult(linkResult, "/testPath");
  }

  @Test
  public void shouldProvideHalLinkForDetailed() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodDetail("TEST", 1, "testTheQuery"));
    assertLinkResult(linkResult, "/testPath/TEST/detail/testTheQuery?query=1");
  }

  @Test
  public void shouldProvideHalLinkForDetailedAndIgnoreQueryParamWithNullValue() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodDetail("TEST", null, "testTheQuery"));
    assertLinkResult(linkResult, "/testPath/TEST/detail/testTheQuery");
  }

  @Test
  public void shouldFailWithoutAnnotation() {
    assertThatThrownBy(
            () -> linkTo(methodOn(TestApi.class).testMethodWithoutPathParamAnnotation("FAIL")))
        .isInstanceOf(org.sda.commons.server.jackson.hal.HalLinkMethodInvocationException.class);
  }

  @Test
  public void shouldDoNothingWhenNoParamsAreProvided() {
    final org.sda.commons.server.jackson.hal.LinkResult linkResult =
        linkTo(methodOn(TestApi.class).testMethodWithoutParams());
    assertLinkResult(linkResult, "/testPathWithNoParams");
  }

  @Test
  public void shouldFailWithNonProxiedMethod() {
    assertThatThrownBy(() -> linkTo("testMethod"))
        .isInstanceOf(org.sda.commons.server.jackson.hal.HalLinkMethodInvocationException.class);
  }

  @Test
  public void shouldFailWithNullProxiedMethod() {
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
