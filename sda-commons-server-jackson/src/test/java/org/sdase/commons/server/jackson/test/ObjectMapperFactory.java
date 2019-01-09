package org.sdase.commons.server.jackson.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.Configuration;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.sdase.commons.server.jackson.JacksonConfigurationBundle;

import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ObjectMapperFactory {

   private ObjectMapperFactory() {
      // Utility class
   }

   /**
    * @return the {@link ObjectMapper} as it would be created in a Dropwizard App using the default
    *         {@link JacksonConfigurationBundle}.
    */
   public static ObjectMapper objectMapperFromBundle() {
      return objectMapperFromBundle(JacksonConfigurationBundle.builder().build());
   }

   /**
    * @param jacksonConfigurationBundle the bundle
    * @return the {@link ObjectMapper} as it would be created in a Dropwizard App using the given
    *         {@code jacksonConfigurationBundle}.
    */
   public static ObjectMapper objectMapperFromBundle(JacksonConfigurationBundle jacksonConfigurationBundle) {
      AtomicReference<ObjectMapper> omRef = new AtomicReference<>();

      // ensure that the ObjectMapper to test is created as it would in a real App
      Bootstrap bootstrapMock = mock(Bootstrap.class, RETURNS_DEEP_STUBS);
      doAnswer(invocation -> {
         omRef.set(invocation.getArgument(0));
         return null;
      }).when(bootstrapMock).setObjectMapper(any(ObjectMapper.class));
      when(bootstrapMock.getObjectMapper()).thenAnswer(invocation -> omRef.get());
      Environment environmentMock = mock(Environment.class, RETURNS_DEEP_STUBS);
      when(environmentMock.getObjectMapper()).thenAnswer(invocation -> omRef.get());
      Configuration configurationMock = mock(Configuration.class, RETURNS_DEEP_STUBS);
      DefaultServerFactory defaultServerFactoryMock = mock(DefaultServerFactory.class);
      when(configurationMock.getServerFactory()).thenReturn(defaultServerFactoryMock);

      jacksonConfigurationBundle.initialize(bootstrapMock);
      jacksonConfigurationBundle.run(configurationMock, environmentMock);

      // we must overwrite Dropwizard's ObjectMapper as early as possible
      verify(bootstrapMock, times(1)).setObjectMapper(omRef.get());
      // we should not use Dropwizard's ObjectMapper as unwanted Modules can't be removed
      verify(bootstrapMock, never()).getObjectMapper();
      // we should get the "default" ObjectMapper for customization in run()
      verify(environmentMock, times(1)).getObjectMapper();
      // we must disable default exception mappers because they are not properly overwritten in a CDI context
      verify(defaultServerFactoryMock, times(1)).setRegisterDefaultExceptionMappers(eq(Boolean.FALSE));

      return omRef.get();
   }

}
