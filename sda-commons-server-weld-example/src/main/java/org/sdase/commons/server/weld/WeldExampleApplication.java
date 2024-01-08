package org.sdase.commons.server.weld;

import io.dropwizard.core.Application;
import io.dropwizard.core.Configuration;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.sdase.commons.server.weld.beans.UsageBean;

@Path("someString")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class WeldExampleApplication extends Application<Configuration> {

  @Inject private UsageBean usageBean;

  @Inject
  @Named("some-string")
  private String someString;

  public static void main(String[] args) throws Exception {
    // activate weld for this application.
    // Do not forget
    // * to annotate the application
    // * to add beans.xml in resources/META-INF
    DropwizardWeldHelper.run(WeldExampleApplication.class, args);
  }

  @Override
  public void initialize(Bootstrap<Configuration> bootstrap) {
    // optional, only if CDI in servlets is needed
    bootstrap.addBundle(new WeldBundle());
  }

  @Override
  public void run(Configuration configuration, Environment environment) {
    // example how to use the bean
    // when you start the application with parameter server, you can find the following line in the
    // log:
    // INFO  [xxxx-xx-xx xx:xx:xx,xxx] org.sdase.commons.server.weld.beans.SimpleBean: do stuff
    // invoked
    // INFO  [xxxx-xx-xx xx:xx:xx,xxx] org.sdase.commons.server.weld.beans.SimpleBean: injected
    // string 'some string'
    usageBean.useSimpleBean();

    // beans that serve requests must be registered
    environment.jersey().register(this);
  }

  @GET
  public String getSomeString() {
    return someString;
  }

  /**
   * Method only for testing. It provides the bean to the test, where it can be verified
   *
   * @return the usage bean
   */
  UsageBean getUsageBean() {
    return usageBean;
  }
}
