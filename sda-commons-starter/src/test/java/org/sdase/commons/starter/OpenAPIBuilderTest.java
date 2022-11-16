package org.sdase.commons.starter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sdase.commons.server.openapi.OpenApiBundle;
import org.sdase.commons.starter.test.BundleAssertion;

class OpenAPIBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @BeforeEach
  void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  void simplestConfig() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, OpenApiBundle.builder().addResourcePackageClass(this.getClass()).build());
  }

  @Test
  void multipleResourcePackages() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .addOpenApiResourcePackageClass(this.getClass())
            .addOpenApiResourcePackage("com.example")
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        OpenApiBundle.builder()
            .addResourcePackageClass(this.getClass())
            .addResourcePackage("com.example")
            .build());
  }

  @Test
  void withExistingOpenAPIFromClasspathResource() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withExistingOpenAPIFromClasspathResource("/example.yaml")
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        OpenApiBundle.builder().withExistingOpenAPIFromClasspathResource("/example.yaml").build());
  }

  @Test
  void withoutOpenApiBundle() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder().usingSdaPlatformConfiguration().build();

    bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, OpenApiBundle.class);
  }
}
