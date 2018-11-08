package org.sdase.commons.server.weld.testing.test.commands;

import org.sdase.commons.server.weld.testing.test.util.BarSupplier;
import io.dropwizard.Configuration;
import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;

import javax.inject.Inject;

public class TestDWCommand extends ConfiguredCommand<Configuration> {

   @Inject
   BarSupplier supplier;

   BarSupplier result;

   public TestDWCommand() {
      super("testDW", "TestDWCommand");
   }

   @Override
   protected void run(Bootstrap<Configuration> bootstrap, Namespace namespace, Configuration configuration)
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
