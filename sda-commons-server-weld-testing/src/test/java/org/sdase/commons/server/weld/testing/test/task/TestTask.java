package org.sdase.commons.server.weld.testing.test.task;

import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

public class TestTask extends Task {

  @Inject BarSupplier supplier;

  BarSupplier result;

  public TestTask() {
    super("runTestTask");
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) throws Exception {
    result = supplier;
  }

  public BarSupplier getSupplier() {
    return supplier;
  }

  public String getResult() {
    return result.get();
  }
}
