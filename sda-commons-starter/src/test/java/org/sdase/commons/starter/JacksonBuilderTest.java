package org.sdase.commons.starter;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Test;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;
import org.sdase.commons.starter.test.BundleAssertion;

public class JacksonBuilderTest {

  private BundleAssertion<SdaPlatformConfiguration> bundleAssertion;

  @Before
  public void setUp() {
    bundleAssertion = new BundleAssertion<>();
  }

  @Test
  public void defaultJacksonConfig() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, JacksonConfigurationBundle.builder().build());
  }

  @Test
  public void noHalSupport() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .withoutHalSupport()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, JacksonConfigurationBundle.builder().withoutHalSupport().build());
  }

  @Test
  public void noFieldFilter() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .withoutFieldFilter()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, JacksonConfigurationBundle.builder().withoutFieldFilter().build());
  }

  @Test
  public void alwaysWithMillis() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .alwaysWriteZonedDateTimeWithMillisInJson()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, JacksonConfigurationBundle.builder().alwaysWriteZonedDateTimeWithMillis().build());
  }

  @Test
  public void alwaysWithoutMillis() {
    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .alwaysWriteZonedDateTimeWithoutMillisInJson()
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle,
        JacksonConfigurationBundle.builder().alwaysWriteZonedDateTimeWithoutMillis().build());
  }

  @Test
  public void withCustomizer() {

    Consumer<ObjectMapper> omc =
        om -> om.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);

    SdaPlatformBundle<SdaPlatformConfiguration> bundle =
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withoutConsumerTokenSupport()
            .addOpenApiResourcePackageClass(this.getClass())
            .withObjectMapperCustomization(omc)
            .build();

    bundleAssertion.assertBundleConfiguredByPlatformBundle(
        bundle, JacksonConfigurationBundle.builder().withCustomization(omc).build());
  }
}
