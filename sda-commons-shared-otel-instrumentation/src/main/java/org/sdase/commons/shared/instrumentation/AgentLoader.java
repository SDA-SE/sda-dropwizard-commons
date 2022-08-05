package org.sdase.commons.shared.instrumentation;

import java.net.URISyntaxException;
import net.bytebuddy.agent.ByteBuddyAgent;
import org.sdase.commons.server.dropwizard.bundles.SystemPropertyAndEnvironmentLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This utils is only responsible for loading the openTelemetry agent. All configuration is already
 * done through environment variables.
 */
public class AgentLoader {

  private static final String MAIN_THREAD_CHECK_ENABLED = "MAIN_THREAD_CHECK_ENABLED";

  private AgentLoader() {
    // utility class
  }

  private static final Logger LOG = LoggerFactory.getLogger(AgentLoader.class);

  public static void load() {
    String jaegerSamplerType = getProperty("JAEGER_SAMPLER_TYPE");
    String jaegerSamplerParam = getProperty("JAEGER_SAMPLER_PARAM");
    String javaAgentDisabled = getProperty("OTEL_JAVAAGENT_ENABLED");
    boolean enableMainThreadCheck = !"false".equals(getProperty(MAIN_THREAD_CHECK_ENABLED));

    String currentTheadName = Thread.currentThread().getName();

    // Skip loading the agent if not triggered from the main thread.
    if (enableMainThreadCheck && !"main".equals(currentTheadName)) {
      LOG.warn("The otel agent is not loaded from the main thread. Skipping instrumentation...");
      return;
    }

    // Skip loading the agent if tracing is disabled.
    if ("const".equals(jaegerSamplerType) && "0".equals(jaegerSamplerParam)) {
      LOG.warn("Tracing is disabled using deprecated configuration.");
      LOG.info("Skipping instrumentation...");
      return;
    }

    // Skip loading the agent if tracing is disabled.
    if ("true".equals(javaAgentDisabled)) {
      LOG.info("Tracing is disabled. Skipping instrumentation...");
      return;
    }
    try {
      LOG.info("Attaching the instrumentation agent to the current jvm...");
      attach();
    } catch (Exception e) {
      // Do not prevent startup.
      LOG.error("Agent failed to attach. ", e);
    }
  }

  private static void attach() throws URISyntaxException {
    ByteBuddyAgent.attach(
        AgentFileProvider.getAgentJarFile(),
        ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve());
  }

  private static String getProperty(String name) {
    return new SystemPropertyAndEnvironmentLookup().lookup(name);
  }
}
