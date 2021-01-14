# SDA Commons Client Jersey WireMock Testing

[![javadoc](https://javadoc.io/badge2/org.sdase.commons/sda-commons-client-jersey-wiremock-testing/javadoc.svg)](https://javadoc.io/doc/org.sdase.commons/sda-commons-client-jersey-wiremock-testing)

Testing dependencies for [WireMock](https://wiremock.org) test framework.

## Extensions

This module provides a Junit 5 extension to setup Wiremock for your tests.

### WireMockExtension

Useful for setting up Wiremock for each one of your tests.

Example:
```java
class WireMockExtensionTest {

  @RegisterExtension
  WiremockExtension wire = new WiremockExtension();

  @BeforeEach
  void before() {
    wire.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));
  }

  // Tests
}
```

### WireMockClassExtension

Useful for setting up Wiremock once for your test class.

Example:
```java
class WireMockClassExtensionTest {

  @RegisterExtension
  public static WireMockClassExtension wire =
      new WireMockClassExtension();

  @BeforeAll
  public static void beforeAll() {
    wire.stubFor(
        get("/api/cars") // NOSONAR
            .withHeader("Accept", notMatching("gzip"))
            .willReturn(ok().withHeader("Content-type", "application/json").withBody("[]")));
  }
  
  // Tests
}
```
