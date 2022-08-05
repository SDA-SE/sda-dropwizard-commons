package org.sdase.commons.shared.instrumentation;

import io.opentelemetry.javaagent.OpenTelemetryAgent;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentFileProvider {
  private AgentFileProvider() {}

  private static final Logger LOG = LoggerFactory.getLogger(AgentFileProvider.class);

  public static File getAgentJarFile() throws URISyntaxException {
    ProtectionDomain protectionDomain = OpenTelemetryAgent.class.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    if (codeSource == null) {
      throw new IllegalStateException(
          String.format("Unable to get agent location, protection domain = %s", protectionDomain));
    }
    URL location = codeSource.getLocation();
    if (location == null) {
      throw new IllegalStateException(
          String.format("Unable to get agent location, code source = %s", codeSource));
    }
    final File agentJar = new File(location.toURI());
    if (!agentJar.getName().endsWith(".jar")) {
      throw new IllegalStateException("Agent is not a jar file: " + agentJar);
    }
    LOG.info("Otel agent found in {}", agentJar.toURI());
    return agentJar.getAbsoluteFile();
  }
}
