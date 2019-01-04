package org.sdase.commons.server.weld;

import io.dropwizard.Application;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.weld.beans.UsageBean;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class WeldExampleApplication extends Application<Configuration> {

   @Inject
   private UsageBean usageBean;

   public static void main(String [] args) throws Exception {
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
      // when you start the application with parameter server, you can find the following line in the log:
      // INFO  [xxxx-xx-xx xx:xx:xx,xxx] org.sdase.commons.server.weld.beans.SimpleBean: do stuff invoked
      // INFO  [xxxx-xx-xx xx:xx:xx,xxx] org.sdase.commons.server.weld.beans.SimpleBean: injected string 'some string'
      usageBean.useSimpleBean();
   }

   /**
    * Method oly for testing. It provides the bean to the test, where it can be verified
    * @return the usage bean
    */
   public UsageBean getUsageBean() {
      return usageBean;
   }

}
