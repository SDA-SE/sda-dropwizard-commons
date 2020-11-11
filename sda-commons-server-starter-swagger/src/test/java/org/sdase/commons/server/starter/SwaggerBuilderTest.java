package org.sdase.commons.server.starter;

import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.starter.test.BundleAssertion;
import org.sdase.commons.server.swagger.SwaggerBundle;

public class SwaggerBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void disabled() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withoutSwagger()
            .build();

    bundleAssertion.assertBundleNotConfiguredByPlatformBundle(bundle, SwaggerBundle.class);
  }

  @Test
  public void simplestConfig() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter") // NOSONAR
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .build());
  }

  @Test
  public void allConfigSimple() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .withSwaggerInfoVersion("1.1.1") // NOSONAR
            .withSwaggerInfoDescription("A test application") // NOSONAR
            .withSwaggerInfoLicense("Sample License") // NOSONAR
            .withSwaggerInfoContact("John Doe") // NOSONAR
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .withVersion("1.1.1")
            .withDescription("A test application")
            .withLicense("Sample License")
            .withContact("John Doe")
            .build());
  }

  @Test
  public void allConfigMedium() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .withSwaggerInfoVersion("1.1.1")
            .withSwaggerInfoDescription("A test application")
            .withSwaggerInfoLicense("Sample License", "http://example.com/license") // NOSONAR
            .withSwaggerInfoContact("John Doe", "j.doe@example.com") // NOSONAR
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .withVersion("1.1.1")
            .withDescription("A test application")
            .withLicense("Sample License", "http://example.com/license")
            .withContact("John Doe", "j.doe@example.com")
            .build());
  }

  @Test
  public void allConfigFullDetail() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .withSwaggerInfoVersion("1.1.1")
            .withSwaggerInfoDescription("A test application")
            .withSwaggerInfoTermsOfServiceUrl("http://example.com/tos")
            .withSwaggerInfoLicense("Sample License", "http://example.com/license")
            .withSwaggerInfoContact(
                "John Doe", "j.doe@example.com", "http://example.com/users/jdoe")
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .withVersion("1.1.1")
            .withDescription("A test application")
            .withTermsOfServiceUrl("http://example.com/tos")
            .withLicense("Sample License", "http://example.com/license")
            .withContact("John Doe", "j.doe@example.com", "http://example.com/users/jdoe")
            .build());
  }

  @Test
  public void noEmbedParameter() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .disableSwaggerEmbedParameter()
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .disableEmbedParameter()
            .build());
  }

  @Test
  public void noJsonExamples() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .disableSwaggerJsonExamples()
            .addSwaggerResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .disableJsonExamples()
            .build());
  }

  @Test
  public void multipleResourcePackages() {

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Starter")
            .addSwaggerResourcePackageClass(this.getClass())
            .addSwaggerResourcePackage("com.example")
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        SwaggerBundle.builder()
            .withTitle("Starter")
            .addResourcePackageClass(this.getClass())
            .addResourcePackage("com.example")
            .build());
  }
}
