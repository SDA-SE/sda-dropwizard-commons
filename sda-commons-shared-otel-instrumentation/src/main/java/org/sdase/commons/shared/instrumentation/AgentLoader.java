package org.sdase.commons.shared.instrumentation;

import io.opentelemetry.contrib.attach.RuntimeAttach;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentLoader {
  private AgentLoader() {
    // utility class
  }

  private static final Logger LOG = LoggerFactory.getLogger(AgentLoader.class);

  public static void load() {
    try {
      LOG.info("Attaching the instrumentation agent to the current jvm...");
      RuntimeAttach.attachJavaagentToCurrentJVM();
    } catch (Exception e) {
      // Do not prevent startup.
      LOG.error("Agent failed to attach.");
    }
  }
}
