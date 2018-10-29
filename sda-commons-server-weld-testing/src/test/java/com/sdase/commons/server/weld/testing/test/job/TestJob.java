package com.sdase.commons.server.weld.testing.test.job;

import com.sdase.commons.server.weld.testing.test.util.BarSupplier;
import de.spinscale.dropwizard.jobs.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;

public class TestJob extends Job {

   @Inject
   BarSupplier supplier;

   @Override
   public void doJob(JobExecutionContext context) throws JobExecutionException {
      // nothing to do...
   }

   public String getResult() {
      return supplier.get();
   }
}
