package org.sdase.commons.server.weld.testing.test;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;

import com.codahale.metrics.health.HealthCheck;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jobs.JobsBundle;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import java.util.List;
import org.sdase.commons.server.jackson.EmbedHelper;
import org.sdase.commons.server.weld.DropwizardWeldHelper;
import org.sdase.commons.server.weld.WeldBundle;
import org.sdase.commons.server.weld.testing.test.commands.TestDWCommand;
import org.sdase.commons.server.weld.testing.test.job.TestJob;
import org.sdase.commons.server.weld.testing.test.resources.DummyResource;
import org.sdase.commons.server.weld.testing.test.servlets.TestServlet;
import org.sdase.commons.server.weld.testing.test.task.TestTask;
import org.sdase.commons.server.weld.testing.test.util.BarSupplier;
import org.sdase.commons.server.weld.testing.test.util.FooLogger;

@ApplicationScoped
public class WeldExampleApplication extends Application<AppConfiguration> {

  @Inject private FooLogger.Foo foo;

  @Inject private Event<FooLogger.Foo> fooEvent;

  @Inject private TestDWCommand testCommand;

  @Inject private BarSupplier supplier;

  @Inject private TestJob testJob;

  @Inject private TestTask testTask;

  private EmbedHelper embedHelper;

  @Override
  public void initialize(final Bootstrap<AppConfiguration> bootstrap) {
    bootstrap.addBundle(new JobsBundle(List.of(testJob)));
    bootstrap.addBundle(new WeldBundle());
    bootstrap.addCommand(testCommand);
  }

  @Override
  public void run(final AppConfiguration config, final Environment environment) {
    environment
        .healthChecks()
        .register(
            "dummy",
            new HealthCheck() {
              @Override
              protected Result check() {
                return Result.healthy("dummy");
              }
            });

    fooEvent.fire(foo);
    environment.jersey().register(DummyResource.class);
    // Necessary now in Weld 6
    TestServlet servlet = CDI.current().select(TestServlet.class).get();
    environment.getApplicationContext().addServlet(servlet, "/foo");
    environment.admin().addTask(testTask);
    this.embedHelper = new EmbedHelper(environment);
  }

  /**
   * Simplified main() to run this example from IDE/maven.
   *
   * @param args CLI arguments
   * @throws Exception on error
   */
  public static void main(String... args) throws Exception {
    DropwizardWeldHelper.run(
        WeldExampleApplication.class, "server", resourceFilePath("test-config.yaml"));
  }

  @Produces
  public EmbedHelper embedHelper() {
    return this.embedHelper;
  }

  public FooLogger.Foo getFoo() {
    return foo;
  }

  public Event<FooLogger.Foo> getFooEvent() {
    return fooEvent;
  }

  public BarSupplier getSupplier() {
    return supplier;
  }

  public TestDWCommand getTestCommand() {
    return testCommand;
  }

  public String getTestJobResult() {
    return testJob.getResult();
  }

  @SuppressWarnings("unused")
  public String getTestTaskResult() {
    return testTask.getResult();
  }
}
