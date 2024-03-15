package org.sdase.commons.server.trace.test;

import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.sdase.commons.shared.tracing.TraceTokenContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TraceTokenAwareExampleTask extends Task {

  private static final Logger LOG = LoggerFactory.getLogger(TraceTokenAwareExampleTask.class);

  public TraceTokenAwareExampleTask() {
    super("example-task");
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) {
    try (var traceTokenContext = TraceTokenContext.getOrCreateTraceTokenContext()) {
      LOG.info("Log with a TraceToken in the MDC.");
      // Note: Response headers will NOT contain the Trace-Token.
      output.println("Trace-Token: %s".formatted(traceTokenContext.get()));

      // Implement the task here. Logs will contain the 'Trace-Token' from the MDC.
    }
  }
}
