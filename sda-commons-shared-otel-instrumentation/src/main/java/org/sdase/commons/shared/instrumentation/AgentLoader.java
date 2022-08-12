package org.sdase.commons.shared.instrumentation;

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
  private static final String RUNTIME_ATTACH_ENABLED = "RUNTIME_ATTACH_ENABLED";
  private static final String JAEGER_SAMPLER_TYPE = "JAEGER_SAMPLER_TYPE";
  private static final String JAEGER_SAMPLER_PARAM = "JAEGER_SAMPLER_PARAM";
  private static final String OTEL_JAVAAGENT_ENABLED = "OTEL_JAVAAGENT_ENABLED";

  private AgentLoader() {
    // utility class
  }

  private static final Logger LOG = LoggerFactory.getLogger(AgentLoader.class);

  public static void load() {
    if (shouldSkipLoading()) {
      return;
    }
    try {
      LOG.info("Attaching the OpenTelemetry agent...");
      ByteBuddyAgent.attach(AgentFileProvider.getAgentJarFile(), getCurrentPid());
    } catch (Exception e) {
      // Do not prevent startup.
      LOG.error("Agent failed to attach. ", e);
    }
  }

  private static boolean shouldSkipLoading() {
    // Skip loading the agent with this module. Can be helpful if users want to use the
    // `--javaagent` flag
    boolean isRunTimeAttachDisabled = "false".equals(getProperty(RUNTIME_ATTACH_ENABLED));
    if (isRunTimeAttachDisabled) {
      LOG.info("Tracing is disabled. Skipping instrumentation...");
      return true;
    }

    // Skip loading the agent if not triggered from the main thread.
    boolean isMainThreadCheckDisabled = "false".equals(getProperty(MAIN_THREAD_CHECK_ENABLED));
    boolean isMainThread = "main".equals(Thread.currentThread().getName());
    if (!isMainThreadCheckDisabled && !isMainThread) {
      LOG.warn("The otel agent is not loaded from the main thread. Skipping instrumentation...");
      return true;
    }

    // Skip loading the agent if tracing is disabled.
    String jaegerSamplerType = getProperty(JAEGER_SAMPLER_TYPE);
    String jaegerSamplerParam = getProperty(JAEGER_SAMPLER_PARAM);
    boolean isDisabledUsingLegacyVars =
        "const".equals(jaegerSamplerType) && "0".equals(jaegerSamplerParam);
    if (isDisabledUsingLegacyVars) {
      LOG.warn("Tracing is disabled using deprecated configuration.");
      LOG.info("Skipping instrumentation...");
      return true;
    }

    // Skip loading the agent if tracing is disabled.
    boolean isJavaAgentDisabled = "false".equals(getProperty(OTEL_JAVAAGENT_ENABLED));
    if (isJavaAgentDisabled) {
      LOG.info("Tracing is disabled. Skipping instrumentation...");
      return true;
    }

    // should not skip
    return false;
  }

  private static String getCurrentPid() {
    return ByteBuddyAgent.ProcessProvider.ForCurrentVm.INSTANCE.resolve();
  }

  private static String getProperty(String name) {
    return new SystemPropertyAndEnvironmentLookup().lookup(name);
  }
}
