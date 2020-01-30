package org.sdase.commons.server.morphia.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.mongodb.MongoClientOptions;
import javax.net.ssl.SSLContext;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.sdase.commons.server.morphia.MongoConfiguration;

public class MongoClientBuilderTest {

  private io.dropwizard.setup.Environment environmentMock;

  @Before
  public void setUp() {
    environmentMock = mock(io.dropwizard.setup.Environment.class, Mockito.RETURNS_DEEP_STUBS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failOnMissingConfiguration() {
    MongoClientBuilder client = new MongoClientBuilder(null);

    client.build(environmentMock);
  }

  @Test
  public void createSslContext() {
    MongoClientOptions.Builder clientOptionsBuilderMock = spy(MongoClientOptions.Builder.class);
    MongoConfiguration mongoConfiguration =
        createValidConfiguration().setUseSsl(true).setCaCertificate("DUMMY");
    MongoClientBuilder mongoClientBuilder =
        new MongoClientBuilder(mongoConfiguration, clientOptionsBuilderMock);

    mongoClientBuilder.build(environmentMock);

    verify(clientOptionsBuilderMock).sslEnabled(true);
    verify(clientOptionsBuilderMock).sslContext(any(SSLContext.class));
  }

  @Test
  public void skipSslContext() {
    MongoClientOptions.Builder clientOptionsBuilderMock = spy(MongoClientOptions.Builder.class);
    MongoConfiguration mongoConfiguration = createValidConfiguration();
    MongoClientBuilder mongoClientBuilder =
        new MongoClientBuilder(mongoConfiguration, clientOptionsBuilderMock);

    mongoClientBuilder.build(environmentMock);

    verify(clientOptionsBuilderMock, never()).sslEnabled(true);
    verify(clientOptionsBuilderMock, never()).sslContext(any(SSLContext.class));
  }

  private static MongoConfiguration createValidConfiguration() {

    MongoConfiguration mongoConfiguration = new MongoConfiguration();

    mongoConfiguration.setDatabase("default_db");
    mongoConfiguration.setHosts("db1.example.net:27017,db2.example.net:2500");
    mongoConfiguration.setOptions("replicaSet=test");
    mongoConfiguration.setUsername("dbuser");
    mongoConfiguration.setPassword("sda123");

    return mongoConfiguration;
  }
}
