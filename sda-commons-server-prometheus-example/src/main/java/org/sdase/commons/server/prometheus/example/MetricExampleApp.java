package org.sdase.commons.server.prometheus.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.sdase.commons.starter.SdaPlatformBundle;
import org.sdase.commons.starter.SdaPlatformConfiguration;

@OpenAPIDefinition(info = @Info(title = "Metric Example App"))
public class MetricExampleApp extends Application<SdaPlatformConfiguration> {

  public static void main(String[] args) throws Exception {
    new MetricExampleApp().run(args);
  }

  @Override
  public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
    bootstrap.addBundle(
        SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .addOpenApiResourcePackageClass(this.getClass())
            .build());
  }

  @Override
  public void run(SdaPlatformConfiguration configuration, Environment environment) {
    MyServiceWithMetrics myService = new MyServiceWithMetrics();

    // just do some operations in my service to generate test data
    for (int i = 0; i < 10; i++) {
      myService.doSomeOperationWithCounting();
      myService.doSomeOperationWithGauge();
      myService.doSomeOperationWithTrackedDuration();
    }
  }
}
