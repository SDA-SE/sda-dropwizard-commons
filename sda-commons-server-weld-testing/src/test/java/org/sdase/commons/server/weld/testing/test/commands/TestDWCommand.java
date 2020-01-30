package org.sdase.commons.server.weld.testing.test.commands;

import io.dropwizard.Configuration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import javax.inject.Inject;
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
      Bootstrap<Configuration> bootstrap, Namespace namespace, Configuration configuration)
      throws Exception {
    result = supplier;
  }

  public BarSupplier getSupplier() {
    return supplier;
  }

  public String getResult() {
    return result.get();
  }
}
