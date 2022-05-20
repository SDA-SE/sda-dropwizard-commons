package org.sdase.commons.shared.instrumentation;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utils is only responsible for loading the openTelemetry agent. All configuration is already
 * done through environment variables.
 */
public class AgentLoader {
  private AgentLoader() {
    // utility class
  }

  private static final Logger LOG = LoggerFactory.getLogger(AgentLoader.class);

  public static void load() {
    String jaegerSamplerType = System.getenv("JAEGER_SAMPLER_TYPE");
    String jaegerSamplerParam = System.getenv("JAEGER_SAMPLER_PARAM");

    // Skip loading the agent if tracing is disabled.
    if ("const".equals(jaegerSamplerType) && "0".equals(jaegerSamplerParam)) {
      LOG.warn("Tracing is disabled using deprecated configuration.");
      LOG.info("Skipping instrumentation...");
      return;
    }
    try {
      LOG.info("Attaching the instrumentation agent to the current jvm...");
      RuntimeAttach.attachJavaagentToCurrentJVM();
    } catch (Exception e) {
      // Do not prevent startup.
      LOG.error("Agent failed to attach. ", e);
    }
  }
}
