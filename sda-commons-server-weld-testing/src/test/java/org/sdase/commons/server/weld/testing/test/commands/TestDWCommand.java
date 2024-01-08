package org.sdase.commons.server.weld.testing.test.commands;

import io.dropwizard.core.Configuration;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import jakarta.inject.Inject;
import net.sourceforge.argparse4j.inf.Namespace;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

public class TestDWCommand extends ConfiguredCommand<Configuration> {

  @Inject BarSupplier supplier;

  BarSupplier result;

  public TestDWCommand() {
    super("testDW", "TestDWCommand");
  }

  @Override
  protected void run(
      Bootstrap<Configuration> bootstrap, Namespace namespace, Configuration configuration) {
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
