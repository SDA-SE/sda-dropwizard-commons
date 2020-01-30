package org.sdase.commons.server.jackson.filter;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.ServiceLocatorProvider;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

public class JacksonFieldFilterModule extends SimpleModule implements Feature {

  private final transient FieldFilterSerializerModifier fieldFilterSerializerModifier;

  /** Use {@link JacksonConfigurationBundle} to create and register a managed instance. */
  public JacksonFieldFilterModule() {
    super("JacksonFieldFilterModule", new Version(0, 0, 0, "", null, null));
    this.fieldFilterSerializerModifier = new FieldFilterSerializerModifier();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.addBeanSerializerModifier(fieldFilterSerializerModifier);
  }

  @Override
  public boolean configure(FeatureContext context) {
    ServiceLocator serviceLocator = ServiceLocatorProvider.getServiceLocator(context);
    serviceLocator.inject(this.fieldFilterSerializerModifier);
    return true;
  }
}
