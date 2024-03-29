package org.sdase.commons.server.jackson.filter;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import org.glassfish.jersey.InjectionManagerProvider;
import org.glassfish.jersey.internal.inject.InjectionManager;
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
    InjectionManager injectionManager = InjectionManagerProvider.getInjectionManager(context);
    injectionManager.inject(this.fieldFilterSerializerModifier);
    return true;
  }
}
