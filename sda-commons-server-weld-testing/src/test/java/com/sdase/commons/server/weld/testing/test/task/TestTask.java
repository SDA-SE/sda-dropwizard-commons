package com.sdase.commons.server.weld.testing.test.task;

import com.google.common.collect.ImmutableMultimap;
import com.sdase.commons.server.weld.testing.test.util.BarSupplier;
import io.dropwizard.servlets.tasks.Task;

import javax.inject.Inject;
import java.io.PrintWriter;

public class TestTask extends Task {

   @Inject
   BarSupplier supplier;

   BarSupplier result;

   public TestTask() {
      super("runTestTask");
   }

   @Override
   public void execute(ImmutableMultimap<String, String> parameters, PrintWriter output) throws Exception {
      result = supplier;

   }

   public BarSupplier getSupplier() {
      return supplier;
   }

   public String getResult() {
      return result.get();
   }

}
