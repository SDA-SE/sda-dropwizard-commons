package org.sdase.commons.server.weld.testing.test.task;

import io.dropwizard.servlets.tasks.Task;
import jakarta.inject.Inject;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

public class TestTask extends Task {

  @Inject BarSupplier supplier;

  BarSupplier result;

  public TestTask() {
    super("runTestTask");
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) {
    result = supplier;
  }

  @SuppressWarnings("unused")
  public BarSupplier getSupplier() {
    return supplier;
  }

  public String getResult() {
    return result.get();
  }
}
