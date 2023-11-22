package org.sdase.commons.server.weld.testing.test.job;

import io.dropwizard.jobs.Job;
import jakarta.inject.Inject;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;

public class TestJob extends Job {

  @Inject BarSupplier supplier;

  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // nothing to do...
  }

  public String getResult() {
    return supplier.get();
  }
}
