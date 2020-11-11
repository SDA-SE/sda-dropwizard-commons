package org.sdase.commons.server.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.openapi.OpenApiBundle;
import org.sdase.commons.server.starter.test.BundleAssertion;

public class OpenAPIBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void simplestConfig() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, OpenApiBundle.builder().addResourcePackageClass(this.getClass()).build());
  }

  @Test
  public void noEmbedParameter() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .addOpenApiResourcePackageClass(this.getClass())
            .disableSwaggerEmbedParameter()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        OpenApiBundle.builder()
            .addResourcePackageClass(this.getClass())
            .disableEmbedParameter()
            .build());
  }

  @Test
  public void multipleResourcePackages() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
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
  public void withExistingOpenAPIFromClasspathResource() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withExistingOpenAPIFromClasspathResource("/example.yaml")
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        OpenApiBundle.builder().withExistingOpenAPIFromClasspathResource("/example.yaml").build());
  }
}
