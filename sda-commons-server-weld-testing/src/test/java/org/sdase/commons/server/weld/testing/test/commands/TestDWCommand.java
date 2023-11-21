package org.sdase.commons.server.weld.testing.test.commands;

import javax.inject.Inject;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
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
