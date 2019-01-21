package org.sdase.commons.server.prometheus.example;

import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.starter.SdaPlatformBundle;
import org.sdase.commons.server.starter.SdaPlatformConfiguration;

public class MetricExampleApp extends Application<SdaPlatformConfiguration> {

   public static void main(String[] args) throws Exception {
      new MetricExampleApp().run(args);
   }

   @Override
   public void initialize(Bootstrap<SdaPlatformConfiguration> bootstrap) {
      bootstrap.addBundle(SdaPlatformBundle.builder()
            .usingSdaPlatformConfiguration()
            .withRequiredConsumerToken()
            .withSwaggerInfoTitle("Metric Example App")
            .addSwaggerResourcePackageClass(this.getClass())
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
