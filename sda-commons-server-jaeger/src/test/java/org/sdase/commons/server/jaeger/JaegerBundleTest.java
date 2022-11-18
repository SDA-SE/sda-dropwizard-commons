package org.sdase.commons.server.jaeger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.opentracing.util.GlobalTracer;
import io.opentracing.util.GlobalTracerTestUtil;
import io.prometheus.client.CollectorRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class JaegerBundleTest {

  @BeforeEach
  @AfterEach
  void cleanUpGlobalTracer() {
    // We have to clean up the global tracer before and after testing:
    // https://github.com/opentracing/opentracing-java/issues/288
    GlobalTracerTestUtil.resetGlobalTracer();
    // We have to clean all metrics, as we do not run the Dropwizard lifecycle hooks during the
    // test:
    CollectorRegistry.defaultRegistry.clear();
  }

  @Test
  void shouldRegisterGlobalTracer() {
    Bootstrap bootstrap = mock(Bootstrap.class);
    Environment environment = mock(Environment.class, Mockito.RETURNS_DEEP_STUBS);
    when(environment.getName()).thenReturn("MyApp");
    Configuration c = new Configuration();

    JaegerBundle jaegerBundle = JaegerBundle.builder().build();
    jaegerBundle.initialize(bootstrap);
    jaegerBundle.run(c, environment);

    assertThat(GlobalTracer.isRegistered()).isTrue();
  }
}
